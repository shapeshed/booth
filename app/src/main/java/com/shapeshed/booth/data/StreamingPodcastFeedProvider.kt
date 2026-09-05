package com.shapeshed.booth.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Discovery-only adapter. Complete refresh and subscription paths continue using [fallback]. */
class StreamingPodcastFeedProvider(
    private val client: OkHttpClient,
    private val parser: StreamingFeedParser,
    private val fallback: PodcastFeedProvider,
    private val metrics: FeedParserMetrics = FeedParserMetrics { },
) {
    suspend fun fetch(
        feedUrl: String,
        onUpdate: suspend (PodcastFeed) -> Unit,
    ): PodcastFeed = withContext(Dispatchers.IO) {
        val canonicalUrl = canonicalFeedUrl(feedUrl)
        val startedAt = System.nanoTime()
        try {
            var parsed: PodcastFeed? = null
            var fallbackRequest: FallbackRequest? = null
            client.newCall(
                Request.Builder()
                    .url(canonicalUrl)
                    .header("Accept", "application/rss+xml, application/atom+xml, application/rdf+xml, application/xml, text/xml")
                    .header("User-Agent", "Booth discovery")
                    .get()
                    .build(),
            ).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val responseEtag = response.header("ETag")
                val responseLastModified = response.header("Last-Modified")
                if (!response.isSuccessful) {
                    fallbackRequest = FallbackRequest(finalUrl, responseEtag, responseLastModified, null)
                } else {
                    val body = response.body
                    var latest: PodcastFeed? = null
                    var failure: FeedParseEvent.Failed? = null
                    val entries = mutableListOf<RssEntry>()
                    var feed: RssFeed? = null
                    try {
                        body.byteStream().use { input ->
                            parser.parse(input, finalUrl).collect { event ->
                                when (event) {
                                    is FeedParseEvent.FeedMetadataAvailable -> {
                                        feed = event.feed
                                        latest = RssParseResult(event.feed, entries.toList()).toPodcastFeed()
                                        onUpdate(latest!!)
                                    }
                                    is FeedParseEvent.EpisodeChunkAvailable -> {
                                        entries += event.entries
                                        latest = feed?.let { RssParseResult(it, entries.toList()).toPodcastFeed() }
                                        latest?.let { onUpdate(it) }
                                    }
                                    is FeedParseEvent.Completed -> latest = event.result.toPodcastFeed()
                                    is FeedParseEvent.Failed -> failure = event
                                    is FeedParseEvent.Warning -> Unit
                                }
                            }
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        failure = FeedParseEvent.Failed(FailureCategory.UNKNOWN, RuntimeException("Streaming discovery parser failed"))
                    }
                    fallbackRequest = failure?.let {
                        FallbackRequest(finalUrl, responseEtag, responseLastModified, it.category)
                    } ?: if (latest == null) {
                        FallbackRequest(finalUrl, responseEtag, responseLastModified, FailureCategory.UNKNOWN)
                    } else {
                        parsed = latest
                        null
                    }
                }
            }
            parsed?.let {
                onUpdate(it)
                recordMetric(FeedParserMetric(FeedParserMode.STREAMING, elapsedMs(startedAt), it.episodes.size))
                return@withContext it
            }
            val request = fallbackRequest ?: FallbackRequest(canonicalUrl, null, null, FailureCategory.UNKNOWN)
            val result = fallback.fetch(request.url, request.etag, request.lastModified)
            onUpdate(result)
            recordMetric(FeedParserMetric(FeedParserMode.FALLBACK, elapsedMs(startedAt), result.episodes.size, request.failureCategory))
            result
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val result = fallback.fetch(canonicalUrl)
            onUpdate(result)
            recordMetric(FeedParserMetric(FeedParserMode.FALLBACK, elapsedMs(startedAt), result.episodes.size))
            result
        }
    }

    private fun recordMetric(metric: FeedParserMetric) {
        runCatching { metrics.record(metric) }
    }

    private fun elapsedMs(startedAt: Long): Long =
        ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(0L)

    private data class FallbackRequest(
        val url: String,
        val etag: String?,
        val lastModified: String?,
        val failureCategory: FailureCategory?,
    )
}
