package com.shapeshed.booth.data

import android.os.Trace
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import com.shapeshed.booth.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import java.util.concurrent.TimeUnit

interface FeedParser {
    suspend fun fetch(
        feedUrl: String,
        etag: String? = null,
        lastModified: String? = null,
    ): RssParseResult
    suspend fun parse(feedUrl: String, body: String): RssParseResult

    suspend fun discover(feedUrl: String): String? = null
}

data class RssParseResult(
    val feed: RssFeed,
    val entries: List<RssEntry>,
    val etag: String? = null,
    val lastModified: String? = null,
    val notModified: Boolean = false,
)

class Prof18FeedParser private constructor(
    private val parser: RssParser,
    private val client: OkHttpClient? = null,
) : FeedParser {
    constructor(client: OkHttpClient) : this(
        RssParserBuilder(callFactory = client).build(),
        client,
    )

    constructor() : this(RssParser())

    override suspend fun fetch(feedUrl: String, etag: String?, lastModified: String?): RssParseResult {
        val client = client ?: return parser.getRssChannel(feedUrl).toParseResult(feedUrl)
        val boundedClient = client.newBuilder()
            .callTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        Trace.beginSection("booth.feed.http")
        try {
            return boundedClient.newCall(
                Request.Builder()
                    .url(feedUrl)
                    .header("Accept", FeedAcceptHeader)
                    .header("User-Agent", FeedUserAgent)
                    .get()
                    .apply {
                        etag?.let { header("If-None-Match", it) }
                        lastModified?.let { header("If-Modified-Since", it) }
                    }
                    .build(),
            ).execute().use { response ->
                val contentType = response.header("Content-Type")
                    ?.substringBefore(';')
                    ?.trim()
                    ?.lowercase()
                if (contentType in HtmlMimeTypes) throw NotAFeedResponseException()
                val finalUrl = response.request.url.toString()
                val responseEtag = response.header("ETag") ?: etag
                val responseLastModified = response.header("Last-Modified") ?: lastModified
                if (response.code == 304) {
                    return@use RssParseResult(
                        feed = RssFeed(
                            id = stableId(finalUrl),
                            title = hostOf(finalUrl).orEmpty().ifBlank { finalUrl },
                            url = finalUrl,
                            siteUrl = null,
                        ),
                        entries = emptyList(),
                        etag = responseEtag,
                        lastModified = responseLastModified,
                        notModified = true,
                    )
                }
                if (!response.isSuccessful) throw FeedHttpException(response.code)
                val responseBody = response.body
                val body = responseBody.byteStream().use { input ->
                    readBoundedText(
                        input = input,
                        charset = responseBody.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8,
                        maxBytes = MaxFeedBytes,
                    )
                }
                Trace.beginSection("booth.feed.parse")
                try {
                    parser.parse(body).toParseResult(finalUrl, body).copy(
                        etag = responseEtag,
                        lastModified = responseLastModified,
                    )
                } finally {
                    Trace.endSection()
                }
            }
        } finally {
            Trace.endSection()
        }
    }

    override suspend fun parse(feedUrl: String, body: String): RssParseResult =
        parser.parse(body).toParseResult(feedUrl, body)

    override suspend fun discover(feedUrl: String): String? {
        val client = client ?: return null
        return runCatching {
            client.newCall(
                Request.Builder()
                    .url(feedUrl)
                    .header("User-Agent", FeedUserAgent)
                    .get()
                    .build(),
            ).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val finalUrl = response.request.url.toString()
                val responseBody = response.body
                val body = responseBody.byteStream().use { input ->
                    readBoundedText(input, Charsets.UTF_8, MaxDiscoveryBytes)
                }
                val document = Jsoup.parse(body, finalUrl)
                val alternate = document.select("link[rel~=(?i)alternate]")
                    .firstOrNull { link ->
                        link.attr("type").lowercase().substringBefore(';') in SupportedFeedMimeTypes
                    }
                    ?.absUrl("href")
                    ?.takeIf(String::isHttpUrl)
                alternate ?: document.select("a[href]")
                    .firstOrNull { link ->
                        link.attr("href").lowercase().substringBefore('?').let { href ->
                            href.contains("rss") || href.contains("atom") || href.contains("feed")
                        }
                    }
                    ?.absUrl("href")
                    ?.takeIf(String::isHttpUrl)
            }
        }.getOrNull()
    }
}

private val SupportedFeedMimeTypes = setOf(
    "application/atom+xml",
    "application/rss+xml",
    "application/rdf+xml",
    "application/xml",
    "text/xml",
)

private val HtmlMimeTypes = setOf("text/html", "application/xhtml+xml")

internal const val FeedAcceptHeader =
    "application/rss+xml, application/atom+xml, application/rdf+xml, application/xml, text/xml, text/html;q=0.8"
internal val FeedUserAgent = "Booth/${BuildConfig.VERSION_NAME} (Android; podcast player)"

class NotAFeedResponseException : IllegalArgumentException("The address returned a web page")

class FeedHttpException(val statusCode: Int) : IllegalArgumentException(
    "Feed request failed with HTTP $statusCode",
)

class FeedResponseException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

class FeedResponseTooLargeException(val maxBytes: Long) : IllegalArgumentException(
    "Feed response exceeded the $maxBytes byte limit",
)

class DuplicateFeedException : IllegalArgumentException("This feed has already been added")

internal fun readBoundedText(input: InputStream, charset: Charset, maxBytes: Long): String {
    require(maxBytes > 0L)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0L
    while (true) {
        val count = input.read(buffer)
        if (count == -1) break
        total += count
        if (total > maxBytes) throw FeedResponseTooLargeException(maxBytes)
        output.write(buffer, 0, count)
    }
    return output.toByteArray().toString(charset)
}

private const val MaxFeedBytes = 16L * 1024L * 1024L
private const val MaxDiscoveryBytes = 2L * 1024L * 1024L

/** RSS uses zero and sometimes negative values when an enclosure size is unknown. */
internal fun positiveSizeOrNull(value: Long?): Long? = value?.takeIf { it > 0L }

suspend fun parseRssFeed(feedUrl: String, body: String): Pair<RssFeed, List<RssEntry>> =
    Prof18FeedParser().parse(feedUrl, body).let { it.feed to it.entries }

private fun RssChannel.toParseResult(feedUrl: String, body: String? = null): RssParseResult {
    val feedTitle = title.cleanText() ?: hostOf(feedUrl).orEmpty().ifBlank { feedUrl }
    val explicit = body?.let(::parseExplicitContent)
    val feed = RssFeed(
        id = stableId(feedUrl),
        title = feedTitle,
        url = feedUrl,
        siteUrl = link.cleanText(),
        description = (description ?: itunesChannelData?.summary).cleanText(),
        imageUrl = (image?.url ?: itunesChannelData?.image)?.absoluteUrl(feedUrl),
        author = itunesChannelData?.author.cleanText(),
        explicit = explicit?.feed,
    )
    val durations = body?.let(::parsePodcastDurations).orEmpty()
    val entries = items.mapIndexedNotNull { index, item ->
        item.toEntry(feed, index)?.copy(
            durationMs = durations.getOrNull(index),
            explicit = explicit?.items?.getOrNull(index) ?: explicit?.feed,
        )
    }
    val videoByEntryId = body?.let { parseVideoEnclosures(it, feedUrl) }.orEmpty()
    return RssParseResult(
        feed = feed,
        entries = entries.map { entry ->
            videoByEntryId[entry.id]?.let { video ->
                entry.copy(
                    videoUrl = video.url,
                    videoMimeType = video.mimeType,
                    videoSizeBytes = video.sizeBytes,
                )
            } ?: entry
        },
    )
}

internal data class ExplicitContent(
    val feed: Boolean?,
    val items: List<Boolean?>,
)

/**
 * Apple defines itunes:explicit on the channel and optionally on each item. The item value is
 * intentionally allowed to override the channel value, including an explicit false value.
 */
internal fun parseExplicitContent(body: String): ExplicitContent {
    val document = Jsoup.parse(body, "", Parser.xmlParser())

    fun org.jsoup.nodes.Element.explicitValue(): Boolean? {
        val explicitElement = children().firstOrNull { child ->
            child.tagName().substringAfterLast(':').equals("explicit", ignoreCase = true)
        } ?: return null
        return when (explicitElement.text().trim().lowercase(Locale.ROOT)) {
            "true", "yes", "1", "explicit" -> true
            "false", "no", "0", "clean" -> false
            else -> null
        }
    }

    val channel = document.selectFirst("channel")
    val itemElements = document.select("channel > item, feed > entry")
    return ExplicitContent(
        feed = channel?.explicitValue() ?: document.selectFirst("feed")?.explicitValue(),
        items = itemElements.map { it.explicitValue() },
    )
}

private fun parsePodcastDurations(body: String): List<Long?> {
    val document = Jsoup.parse(body, "", Parser.xmlParser())
    return document.select("item, entry").map { item ->
        item.children()
            .firstOrNull { child ->
                child.tagName().lowercase(Locale.ROOT).let { tag ->
                    tag == "duration" || tag.endsWith(":duration")
                }
            }
            ?.text()
            ?.parsePodcastDuration()
    }
}

internal fun String.parsePodcastDuration(): Long? {
    val value = trim()
    if (value.isBlank()) return null
    value.toLongOrNull()?.takeIf { it >= 0L }?.let { return it * 1_000L }
    val parts = value.split(':').map { it.toLongOrNull() ?: return null }
    if (parts.isEmpty() || parts.any { it < 0L }) return null
    val seconds = when (parts.size) {
        2 -> parts[0] * 60L + parts[1]
        3 -> parts[0] * 3_600L + parts[1] * 60L + parts[2]
        else -> return null
    }
    return seconds * 1_000L
}

private fun RssItem.toEntry(feed: RssFeed, feedOrder: Int): RssEntry? {
    val guidValue = guid.cleanText()
    val linkedUrl = link.cleanText()
        ?.absoluteUrl(feed.url)
        ?.takeIf(String::isHttpUrl)
    val guidUrl = guidValue?.takeIf(String::isHttpUrl)
    val enclosureUrl = (rawEnclosure?.url ?: audio)
        ?.absoluteUrl(feed.url)
        ?.takeIf(String::isHttpUrl)
    val articleUrl = linkedUrl
        ?: guidUrl
        ?: enclosureUrl
        ?: feed.siteUrl?.absoluteUrl(feed.url)?.takeIf(String::isHttpUrl)
        ?: feed.url.takeIf(String::isHttpUrl)
        ?: return null
    val summary = content.cleanText()
        ?: description.cleanText()
        ?: itunesItemData?.summary.cleanText()
    val image = normalizedImage(articleUrl, summary, itunesItemData?.image)
    return RssEntry(
        id = stableId(guidValue ?: linkedUrl ?: articleUrl),
        feedId = feed.id,
        feedTitle = feed.title,
        title = title.cleanText() ?: articleUrl,
        feedOrder = feedOrder,
        url = articleUrl,
        author = author.cleanText(),
        publishedAtMillis = pubDate.cleanText()?.parseFeedDateMillis(),
        summaryHtml = summary,
        imageUrl = image.url,
        imageSource = image.source,
        audioUrl = enclosureUrl,
        audioMimeType = rawEnclosure?.type,
        audioSizeBytes = positiveSizeOrNull(rawEnclosure?.length),
        webUrl = linkedUrl ?: guidUrl,
    )
}

internal data class ParsedVideoEnclosure(
    val entryId: Long,
    val url: String,
    val mimeType: String,
    val sizeBytes: Long?,
)

/**
 * rssparser intentionally keeps extension tags opaque. Read only the stable video enclosure
 * fields here; normal RSS parsing remains owned by rssparser.
 */
internal fun parseVideoEnclosures(body: String, feedUrl: String): Map<Long, ParsedVideoEnclosure> {
    val itemPattern = Regex(
        "<(?:[A-Za-z0-9_-]+:)?(?:item|entry)\\b[^>]*>(.*?)</(?:[A-Za-z0-9_-]+:)?(?:item|entry)>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    val tagText = fun(block: String, name: String): String? = Regex(
        "<(?:[A-Za-z0-9_-]+:)?$name\\b[^>]*>(.*?)</(?:[A-Za-z0-9_-]+:)?$name>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    ).find(block)?.groupValues?.getOrNull(1)?.replace(Regex("<!\\[CDATA\\[|\\]\\]>"), "")?.trim()
    val attribute = fun(attributes: String, name: String): String? = Regex(
        "\\b$name\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']",
        RegexOption.IGNORE_CASE,
    ).find(attributes)?.groupValues?.getOrNull(1)?.trim()

    return itemPattern.findAll(body).mapNotNull { match ->
        val block = match.groupValues[1]
        val identity = tagText(block, "guid") ?: tagText(block, "id") ?: tagText(block, "link")
            ?: tagText(block, "title") ?: return@mapNotNull null
        val alternatePattern = Regex(
            "<(?:[A-Za-z0-9_-]+:)?alternateEnclosure\\b([^>]*)>(.*?)</(?:[A-Za-z0-9_-]+:)?alternateEnclosure>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val alternate = alternatePattern.findAll(block).mapNotNull { enclosure ->
            val attributes = enclosure.groupValues[1]
            val type = attribute(attributes, "type")?.lowercase() ?: return@mapNotNull null
            if (!type.startsWith("video/") && type !in HlsMimeTypes) return@mapNotNull null
            val contents = enclosure.groupValues[2]
            val rawUrl = Regex(
                "<(?:[A-Za-z0-9_-]+:)?source\\b[^>]*\\buri\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']",
                RegexOption.IGNORE_CASE,
            ).find(contents)?.groupValues?.getOrNull(1)
                ?: attribute(attributes, "url")
                ?: return@mapNotNull null
            val url = rawUrl.absoluteUrl(feedUrl)?.takeIf(String::isHttpUrl) ?: return@mapNotNull null
            ParsedVideoEnclosure(
                stableId(identity),
                url,
                type,
                positiveSizeOrNull(attribute(attributes, "length")?.toLongOrNull()),
            )
        }.sortedBy { it.mimeType !in HlsMimeTypes }.firstOrNull()
            ?: Regex(
                "<(?:[A-Za-z0-9_-]+:)?enclosure\\b([^>]*)/?>",
                RegexOption.IGNORE_CASE,
            ).findAll(block).mapNotNull { enclosure ->
                val attributes = enclosure.groupValues[1]
                val type = attribute(attributes, "type")?.lowercase()
                    ?.takeIf { it.startsWith("video/") } ?: return@mapNotNull null
                val url = attribute(attributes, "url")?.absoluteUrl(feedUrl)?.takeIf(String::isHttpUrl)
                    ?: return@mapNotNull null
                ParsedVideoEnclosure(
                    stableId(identity),
                    url,
                    type,
                    positiveSizeOrNull(attribute(attributes, "length")?.toLongOrNull()),
                )
            }.firstOrNull()
        alternate?.let { it.entryId to it }
    }.toMap()
}

private val HlsMimeTypes = setOf(
    "application/vnd.apple.mpegurl",
    "application/x-mpegurl",
    "application/x-mpegURL".lowercase(),
)

private data class RssImageCandidate(
    val url: String?,
    val source: RssImageSource,
)

private fun RssItem.normalizedImage(
    articleUrl: String,
    summaryHtml: String?,
    itunesImage: String?,
): RssImageCandidate =
    sequenceOf(
        // Prefer the podcast-specific episode artwork when supplied. Some feeds,
        // including TWiT, publish a complete image here alongside a cropped
        // media:thumbnail intended for small list previews.
        RssImageCandidate(itunesImage, RssImageSource.FEED),
        // Media RSS <media:thumbnail> is exposed here by rssparser. BBC News uses that field.
        RssImageCandidate(image.cleanText(), RssImageSource.FEED),
        RssImageCandidate(rawMediaContent?.url.takeIfImage(rawMediaContent?.type, rawMediaContent?.medium), RssImageSource.FEED),
        RssImageCandidate(
            (rawEnclosure?.url ?: audio).takeIfImage(rawEnclosure?.type, null),
            RssImageSource.FEED,
        ),
        RssImageCandidate(summaryHtml.firstImageFromHtml(articleUrl), RssImageSource.BODY),
    ).firstNotNullOfOrNull { candidate ->
        candidate.url?.absoluteUrl(articleUrl)?.let { candidate.copy(url = it) }
    } ?: RssImageCandidate(url = null, source = RssImageSource.NONE)

private fun String?.takeIfImage(type: String?, medium: String?): String? {
    val cleaned = cleanText() ?: return null
    val normalizedType = type.orEmpty().lowercase()
    val normalizedMedium = medium.orEmpty().lowercase()
    val path = cleaned.substringBefore('?').lowercase()
    return cleaned.takeIf {
        normalizedType.startsWith("image/") ||
            normalizedMedium == "image" ||
            path.endsWith(".jpg") ||
            path.endsWith(".jpeg") ||
            path.endsWith(".png") ||
            path.endsWith(".webp") ||
            path.endsWith(".gif")
    }
}

private fun String?.firstImageFromHtml(baseUrl: String): String? {
    val html = cleanText() ?: return null
    val image = Jsoup.parse(html, baseUrl).selectFirst("img[src], img[data-src], img[data-original]") ?: return null
    return image.attr("abs:src").cleanText()
        ?: image.attr("abs:data-src").cleanText()
        ?: image.attr("abs:data-original").cleanText()
}

internal fun String.absoluteUrl(baseUrl: String): String? {
    val cleaned = cleanText() ?: return null
    cleaned.toHttpUrlOrNull()?.let { return it.toString() }
    val base = baseUrl.toHttpUrlOrNull() ?: return cleaned
    return base.resolve(cleaned)?.toString() ?: cleaned
}

private fun String.isHttpUrl(): Boolean = toHttpUrlOrNull()?.scheme in setOf("http", "https")

internal fun String?.cleanText(): String? = this?.trim()?.takeIf(String::isNotBlank)

internal fun String.parseFeedDateMillis(): Long? =
    sequenceOf(
        { ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() },
        { ZonedDateTime.parse(this, RssDateTimeWithZoneName).toInstant().toEpochMilli() },
        { OffsetDateTime.parse(this, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() },
    ).firstNotNullOfOrNull { parser -> runCatching(parser).getOrNull() }

private val RssDateTimeWithZoneName: DateTimeFormatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendPattern("EEE, d MMM yyyy HH:mm:ss zzz")
    .toFormatter(Locale.US)

fun stableFeedId(value: String): Long = stableId(canonicalFeedUrl(value))

fun canonicalFeedUrl(value: String): String {
    val normalized = value.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
    return normalized.toHttpUrlOrNull()
        ?.newBuilder()
        ?.fragment(null)
        ?.build()
        ?.toString()
        ?: normalized
}

private fun stableId(value: String): Long {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    var result = 0L
    for (index in 0 until Long.SIZE_BYTES) {
        result = (result shl 8) or (digest[index].toLong() and 0xff)
    }
    return result and Long.MAX_VALUE
}
