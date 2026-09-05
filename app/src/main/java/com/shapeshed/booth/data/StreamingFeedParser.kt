package com.shapeshed.booth.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.xml.sax.Attributes
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.ext.DefaultHandler2
import java.io.InputStream
import java.io.StringReader
import java.util.Locale
import javax.xml.parsers.SAXParserFactory

private const val AtomNamespace = "http://www.w3.org/2005/Atom"
private const val RdfNamespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
private const val RssNamespace = "http://purl.org/rss/1.0/"
private const val ItunesNamespace = "http://www.itunes.com/dtds/podcast-1.0.dtd"
private const val ContentNamespace = "http://purl.org/rss/1.0/modules/content/"
private const val MediaRssNamespace = "http://search.yahoo.com/mrss/"
private const val DublinCoreNamespace = "http://purl.org/dc/elements/1.1/"
private const val XmlNamespace = "http://www.w3.org/XML/1998/namespace"

private val BasicItemFields = setOf(
    "guid", "id", "identifier", "title", "link", "description", "summary",
    "pubdate", "published", "updated", "author", "creator", "name", "image",
    "duration", "explicit",
)
private val RssItemFields = setOf("title", "link", "description", "date")
private val AtomItemFields = setOf(
    "id", "title", "link", "summary", "content", "published", "updated", "name",
)
private val ItunesItemFields = setOf(
    "title", "summary", "subtitle", "author", "image", "duration", "explicit",
)
private val DublinCoreItemFields = setOf("identifier", "creator", "date")
private val BasicFeedFields = setOf("title", "link", "description", "summary", "image", "url", "explicit")
private val RssFeedFields = setOf("title", "link", "description")
private val AtomFeedFields = setOf("title", "link", "summary", "subtitle", "name")
private val ItunesFeedFields = setOf("image", "author", "summary", "subtitle", "explicit", "name")
private const val FeedImageContainerField = "__feed_image_container"
private const val FeedImageUrlField = "__feed_image_url"

/** Namespace-aware, Android-safe SAX parser. It intentionally has no network-capable XML features. */
class SaxStreamingFeedParser(
    private val maxBytes: Long = 16L * 1024L * 1024L,
    private val maxItems: Int = 10_000,
    private val maxText: Int = 512 * 1024,
    private val maxDepth: Int = 128,
    private val firstChunkSize: Int = 10,
    private val chunkSize: Int = 25,
) : StreamingFeedParser {
    init {
        require(maxBytes > 0) { "maxBytes must be positive" }
        require(maxItems > 0) { "maxItems must be positive" }
        require(maxText > 0) { "maxText must be positive" }
        require(maxDepth > 0) { "maxDepth must be positive" }
        require(firstChunkSize > 0) { "firstChunkSize must be positive" }
        require(chunkSize > 0) { "chunkSize must be positive" }
    }

    override fun parse(input: InputStream, feedUrl: String): Flow<FeedParseEvent> = channelFlow {
        val parsingContext = currentCoroutineContext()
        val handler = Handler(feedUrl, maxItems, maxText, maxDepth, firstChunkSize, chunkSize,
            { parsingContext.ensureActive() }, { event ->
                if (!trySend(event).isSuccess) {
                    throw kotlinx.coroutines.CancellationException("Streaming feed consumer was cancelled")
                }
            })
        try {
            withContext(Dispatchers.IO) {
                val factory = SAXParserFactory.newInstance().apply {
                    isNamespaceAware = true
                    setFeature("http://xml.org/sax/features/external-general-entities", false)
                    setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                }
                factory.newSAXParser().xmlReader.apply {
                    contentHandler = handler
                    errorHandler = handler
                    runCatching {
                        setProperty("http://xml.org/sax/properties/lexical-handler", handler)
                    }
                    entityResolver = EntityResolver { _, _ -> InputSource(StringReader("")) }
                    parse(InputSource(LimitedInputStream(input, maxBytes)))
                }
            }
            handler.flush()
            send(FeedParseEvent.Completed(handler.result()))
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            val category = when (t) {
                is FeedResponseTooLargeException -> FailureCategory.LIMIT
                is ParserLimitException -> FailureCategory.LIMIT
                is SAXException, is SAXParseException -> FailureCategory.STRUCTURAL
                else -> FailureCategory.UNKNOWN
            }
            send(FeedParseEvent.Failed(category, t))
        }
    }.buffer(Channel.UNLIMITED)

    private class LimitedInputStream(private val delegate: InputStream, private val limit: Long) : InputStream() {
        private var count = 0L
        override fun read(): Int = delegate.read().also { if (it >= 0 && ++count > limit) throw FeedResponseTooLargeException(limit) }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val read = delegate.read(buffer, offset, length)
            if (read > 0 && (count + read) > limit) throw FeedResponseTooLargeException(limit)
            if (read > 0) count += read
            return read
        }
        override fun close() = delegate.close()
    }

    private class Handler(
        private val url: String,
        private val maxItems: Int,
        private val maxText: Int,
        private val maxDepth: Int,
        private val firstChunkSize: Int,
        private val chunkSize: Int,
        private val checkCancellation: () -> Unit,
        private val publish: (FeedParseEvent) -> Unit,
    ) : DefaultHandler2() {
        private val entries = mutableListOf<RssEntry>()
        private val pending = mutableListOf<RssEntry>()
        private var feedTitle: String? = null
        private var feedLink: String? = null
        private var feedDescription: String? = null
        private var feedImage: String? = null
        private var feedAuthor: String? = null
        private var feedExplicit: Boolean? = null
        private var current: Item? = null
        private var field: String? = null
        private var text = StringBuilder()
        private var rootSeen = false
        private var format: FeedFormat? = null
        private var feedEventSent = false
        private var elementDepth = 0
        private val elementBases = mutableListOf<String>()
        private var fieldDepth = -1
        private var fieldBaseUrl = url
        private var feedImageNestedUrlCaptured = false

        override fun startElement(uri: String?, localName: String?, qName: String?, attrs: Attributes) {
            checkCancellation()
            val name = localName.orEmpty().ifBlank { qName.orEmpty().substringAfter(':') }.lowercase(Locale.ROOT)
            val parentBaseUrl = elementBases.lastOrNull() ?: url
            val elementBaseUrl = attrs.getValue(XmlNamespace, "base")
                ?.absoluteUrl(parentBaseUrl)
                ?: parentBaseUrl
            elementBases += elementBaseUrl
            elementDepth++
            if (elementDepth > maxDepth) {
                throw ParserLimitException("XML nesting depth", maxDepth.toLong())
            }
            if (!rootSeen) {
                rootSeen = true
                format = when (name) {
                    "rss" if uri.orEmpty().isBlank() -> FeedFormat.RSS
                    "rdf" if uri.orEmpty() == RdfNamespace -> FeedFormat.RDF
                    "feed" if uri.orEmpty() == AtomNamespace -> FeedFormat.ATOM
                    else -> throw SAXException("Unsupported feed root: $name")
                }
            }
            if (isItemElement(uri.orEmpty(), name)) {
                if (current != null) throw SAXException("Nested feed item")
                if (entries.size >= maxItems) throw FeedResponseTooLargeException(maxItems.toLong())
                if (!feedEventSent) emitFeed()
                current = Item(elementBaseUrl)
                return
            }
            if (current != null) {
                if (name == "enclosure" ||
                    (name == "link" && attrs.getValue("rel").equals("enclosure", ignoreCase = true))
                ) {
                    current!!.chooseEnclosure(Enclosure(
                        (attrs.getValue("url") ?: attrs.getValue("href"))?.absoluteUrl(elementBaseUrl),
                        attrs.getValue("type"),
                        attrs.getValue("length")?.toLongOrNull(),
                    ))
                }
                if (isItemField(uri.orEmpty(), name) &&
                    !(uri == MediaRssNamespace && name == "content" && !isMediaImageElement(name, attrs)) &&
                    fieldDepth < 0
                ) {
                    if (name == "link" && attrs.getValue("href") != null) {
                        current!!.setAtomLink(attrs.getValue("rel"), attrs.getValue("href"), elementBaseUrl)
                    }
                    if (name == "image" ||
                        (uri == MediaRssNamespace && isMediaImageElement(name, attrs))
                    ) {
                        current!!.setImage(
                            (attrs.getValue("href") ?: attrs.getValue("url"))
                                ?.absoluteUrl(elementBaseUrl),
                            uri.orEmpty(),
                        )
                    }
                    field = name
                    fieldDepth = elementDepth
                    fieldBaseUrl = elementBaseUrl
                    text = StringBuilder()
                }
            } else if (
                field == FeedImageContainerField &&
                name == "url" &&
                elementDepth == fieldDepth + 1
            ) {
                field = FeedImageUrlField
                fieldDepth = elementDepth
                fieldBaseUrl = elementBaseUrl
                text = StringBuilder()
            } else if (isFeedField(uri.orEmpty(), name) && fieldDepth < 0) {
                if (name == "link" && attrs.getValue("href") != null) {
                    feedLink = attrs.getValue("href")?.absoluteUrl(elementBaseUrl)
                }
                if (name == "image") {
                    feedImage = (attrs.getValue("href") ?: attrs.getValue("url"))?.absoluteUrl(elementBaseUrl)
                    field = FeedImageContainerField
                    feedImageNestedUrlCaptured = false
                } else {
                    field = name
                }
                fieldDepth = elementDepth
                fieldBaseUrl = elementBaseUrl
                text = StringBuilder()
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (field == FeedImageContainerField && feedImageNestedUrlCaptured) return
            if (field != null && text.length < maxText) text.append(ch, start, minOf(length, maxText - text.length))
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            checkCancellation()
            val name = localName.orEmpty().ifBlank { qName.orEmpty().substringAfter(':') }.lowercase(Locale.ROOT)
            val value = text.toString().trim()
            if (current != null) {
                val item = current!!
                if (isItemElement(uri.orEmpty(), name)) {
                    item.finish(url, feed(), entries.size)?.let { entry ->
                        entries += entry
                        pending += entry
                        if (!feedEventSent) emitFeed()
                        if (pending.size >= if (entries.size <= firstChunkSize) firstChunkSize else chunkSize) emitChunk()
                    } ?: publish(FeedParseEvent.Warning("Skipped item without a usable URL"))
                    current = null
                    clearField()
                } else if (fieldDepth == elementDepth && field == name) {
                    item.set(name, value, uri.orEmpty(), fieldBaseUrl)
                    clearField()
                }
            } else if (field == FeedImageUrlField && fieldDepth == elementDepth && name == "url") {
                if (value.isNotBlank()) feedImage = value.absoluteUrl(fieldBaseUrl)
                feedImageNestedUrlCaptured = true
                field = FeedImageContainerField
                fieldDepth = elementDepth - 1
                text = StringBuilder()
            } else if (field == FeedImageContainerField && fieldDepth == elementDepth && name == "image") {
                if (!feedImageNestedUrlCaptured && value.isNotBlank()) {
                    feedImage = value.absoluteUrl(fieldBaseUrl)
                }
                clearField()
                feedImageNestedUrlCaptured = false
            } else if (fieldDepth == elementDepth && field == name) {
                when (name) {
                    "title" -> feedTitle = value
                    "link" -> value.absoluteUrl(fieldBaseUrl)?.let { feedLink = it }
                    "description", "summary" -> if (feedDescription == null) feedDescription = value
                    "author", "name" -> if (feedAuthor == null) feedAuthor = value
                    "url" -> if (value.isNotBlank()) feedImage = value.absoluteUrl(fieldBaseUrl)
                    "explicit" -> feedExplicit = parseFeedBoolean(value)
                }
                clearField()
            }
            elementBases.removeLastOrNull()
            elementDepth--
        }

        private fun clearField() {
            field = null
            fieldDepth = -1
            fieldBaseUrl = url
            text = StringBuilder()
        }

        private fun isItemElement(namespace: String, name: String): Boolean = when (format) {
            FeedFormat.ATOM -> namespace == AtomNamespace && name == "entry"
            FeedFormat.RSS -> namespace.isBlank() && name == "item"
            FeedFormat.RDF -> name == "item" && (namespace.isBlank() || namespace == RssNamespace)
            null -> false
        }

        private fun isItemField(namespace: String, name: String): Boolean = when {
            namespace.isBlank() -> name in BasicItemFields
            namespace == RssNamespace -> name in RssItemFields
            namespace == AtomNamespace -> name in AtomItemFields
            namespace == ItunesNamespace -> name in ItunesItemFields
            namespace == ContentNamespace -> name == "encoded"
            namespace == MediaRssNamespace -> name == "content" || name == "thumbnail"
            namespace == DublinCoreNamespace -> name in DublinCoreItemFields
            else -> false
        }

        private fun isFeedField(namespace: String, name: String): Boolean = when {
            namespace.isBlank() -> name in BasicFeedFields
            namespace == RssNamespace -> name in RssFeedFields
            namespace == AtomNamespace -> name in AtomFeedFields
            namespace == ItunesNamespace -> name in ItunesFeedFields
            namespace == ContentNamespace -> name == "encoded"
            else -> false
        }

        private fun isMediaImageElement(name: String, attrs: Attributes): Boolean =
            name == "thumbnail" ||
                attrs.getValue("medium").equals("image", ignoreCase = true) ||
                attrs.getValue("type").orEmpty().startsWith("image/", ignoreCase = true)

        private fun emitFeed() {
            feedEventSent = true
            publish(FeedParseEvent.FeedMetadataAvailable(feed()))
        }

        private fun emitChunk() {
            if (pending.isNotEmpty()) {
                publish(FeedParseEvent.EpisodeChunkAvailable(pending.toList()))
                pending.clear()
            }
        }

        fun flush() { if (!feedEventSent) emitFeed(); emitChunk() }

        private fun feed() = RssFeed(
            id = stableFeedId(url),
            title = feedTitle?.cleanText() ?: hostOf(url).orEmpty().ifBlank { url },
            url = url,
            siteUrl = feedLink?.absoluteUrl(url),
            description = feedDescription,
            imageUrl = feedImage?.absoluteUrl(url),
            author = feedAuthor,
            explicit = feedExplicit,
        )

        fun result() = RssParseResult(feed = feed(), entries = entries.toList())

        override fun warning(exception: SAXParseException?) { publish(FeedParseEvent.Warning(exception?.message ?: "XML warning")) }

        override fun startDTD(name: String?, publicId: String?, systemId: String?) {
            throw SAXException("DOCTYPE is not supported")
        }
        override fun error(exception: SAXParseException?) { throw exception ?: SAXException("XML error") }
        override fun fatalError(exception: SAXParseException?) { throw exception ?: SAXException("Malformed XML") }
    }

    private data class Enclosure(val url: String?, val type: String?, val length: Long?)

    private class Item(private val baseUrl: String) {
        var guid: String? = null
        var title: String? = null
        var link: String? = null
        var alternateLink: String? = null
        var summary: String? = null
        var author: String? = null
        var date: String? = null
        var image: String? = null
        var enclosure: Enclosure? = null
        var duration: String? = null
        var explicit: Boolean? = null

        fun chooseEnclosure(candidate: Enclosure) {
            val candidateUrl = candidate.url?.toHttpUrlOrNull()
            if (candidateUrl?.scheme != "http" && candidateUrl?.scheme != "https") return
            val currentUrl = enclosure?.url?.toHttpUrlOrNull()
            val currentType = enclosure?.type.orEmpty()
            val candidateType = candidate.type.orEmpty()
            if (currentUrl == null ||
                (!currentType.startsWith("audio/") && candidateType.startsWith("audio/")
                    )
            ) {
                enclosure = candidate
            }
        }

        fun setAtomLink(relation: String?, href: String, elementBaseUrl: String) {
            when (relation?.lowercase(Locale.ROOT)) {
                "enclosure" -> Unit
                null, "", "alternate" -> alternateLink = href.absoluteUrl(elementBaseUrl)
                else -> Unit
            }
        }

        fun setImage(candidate: String?, namespace: String) {
            if (candidate.isNullOrBlank()) return
            if (namespace == ItunesNamespace || image == null) image = candidate
        }

        fun set(name: String, value: String, namespace: String, elementBaseUrl: String) {
            when (namespace to name) {
                "" to "guid", "" to "id", "http://www.w3.org/2005/Atom" to "id",
                "http://purl.org/dc/elements/1.1/" to "identifier" -> guid = value
                "" to "title", "http://purl.org/rss/1.0/" to "title",
                "http://www.w3.org/2005/Atom" to "title" -> title = value
                "" to "link", "http://purl.org/rss/1.0/" to "link" ->
                    if (value.isNotBlank()) link = value.absoluteUrl(elementBaseUrl) ?: value
                "" to "description",
                "http://purl.org/rss/1.0/" to "description",
                "http://www.w3.org/2005/Atom" to "summary",
                "http://www.w3.org/2005/Atom" to "content",
                "http://purl.org/rss/1.0/modules/content/" to "encoded" -> if (value.isNotBlank()) summary = value
                "" to "pubdate", "" to "published", "" to "updated",
                "http://purl.org/rss/1.0/" to "date",
                "http://www.w3.org/2005/Atom" to "published", "http://www.w3.org/2005/Atom" to "updated",
                "http://purl.org/dc/elements/1.1/" to "date" -> date = value
                "" to "author", "" to "name", AtomNamespace to "name",
                "http://purl.org/dc/elements/1.1/" to "creator" -> author = value
                ItunesNamespace to "title" -> if (title.isNullOrBlank() && value.isNotBlank()) title = value
                ItunesNamespace to "summary",
                ItunesNamespace to "subtitle" -> if (summary.isNullOrBlank() && value.isNotBlank()) summary = value
                ItunesNamespace to "author" -> if (author.isNullOrBlank() && value.isNotBlank()) author = value
                ItunesNamespace to "image",
                MediaRssNamespace to "content",
                MediaRssNamespace to "thumbnail" -> {
                    setImage(value, namespace)
                }
                ItunesNamespace to "duration" -> duration = value
                ItunesNamespace to "explicit" -> explicit = parseFeedBoolean(value)
            }
        }

        fun finish(url: String, feed: RssFeed, order: Int): RssEntry? {
            val articleUrl = listOfNotNull(
                alternateLink?.cleanText(),
                link?.cleanText(),
                enclosure?.url?.cleanText(),
                guid?.cleanText()?.takeIf { it.startsWith("http://") || it.startsWith("https://") },
            )
                .asSequence()
                .mapNotNull { it.absoluteUrl(url) }
                .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
                ?: return null
            return RssEntry(
                id = stableFeedId(guid ?: articleUrl),
                feedId = feed.id,
                feedTitle = feed.title,
                title = title?.ifBlank { articleUrl } ?: articleUrl,
                feedOrder = order,
                url = articleUrl,
                author = author,
                publishedAtMillis = date?.parseFeedDateMillis(),
                summaryHtml = summary,
                imageUrl = image?.absoluteUrl(baseUrl),
                imageSource = if (image == null) RssImageSource.NONE else RssImageSource.FEED,
                audioUrl = enclosure?.url?.absoluteUrl(url),
                audioMimeType = enclosure?.type,
                audioSizeBytes = positiveSizeOrNull(enclosure?.length),
                webUrl = (alternateLink ?: link)?.absoluteUrl(url),
                durationMs = duration?.parsePodcastDuration(),
                explicit = explicit,
            )
        }

    }

}

private class ParserLimitException(
    val limitName: String,
    val limit: Long,
) : IllegalArgumentException("Parser limit exceeded: $limitName ($limit)")

private fun parseFeedBoolean(value: String): Boolean? = when (value.lowercase(Locale.ROOT)) {
    "true", "yes", "1", "explicit" -> true
    "false", "no", "0", "clean" -> false
    else -> null
}
