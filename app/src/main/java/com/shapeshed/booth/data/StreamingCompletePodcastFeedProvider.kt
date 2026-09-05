package com.shapeshed.booth.data

import android.os.Trace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Complete-feed adapter for the SAX parser. The legacy parser remains the compatibility
 * fallback, while persistence continues to happen only after a complete result is available.
 */
class StreamingCompletePodcastFeedProvider(
    private val client: OkHttpClient,
    private val parser: StreamingFeedParser,
    private val fallback: PodcastFeedProvider,
    private val metrics: FeedParserMetrics = FeedParserMetrics { },
) : PodcastFeedProvider {
    override suspend fun fetch(
        feedUrl: String,
        etag: String?,
        lastModified: String?,
        onEpisodeProgress: (processed: Int, total: Int) -> Unit,
    ): PodcastFeed = withContext(Dispatchers.IO) {
        val canonicalUrl = canonicalFeedUrl(feedUrl)
        val startedAt = System.nanoTime()
        try {
            val parsed = fetchWithStreamingParser(
                feedUrl = canonicalUrl,
                etag = etag,
                lastModified = lastModified,
                onEpisodeProgress = onEpisodeProgress,
            )
            recordMetric(FeedParserMetric(FeedParserMode.STREAMING, elapsedMs(startedAt), parsed.episodes.size))
            parsed
        } catch (error: CancellationException) {
            throw error
        } catch (error: StreamingParserFailureException) {
            val parsed = fallback.fetch(error.feedUrl, error.etag, error.lastModified, onEpisodeProgress)
            recordMetric(
                FeedParserMetric(
                    mode = FeedParserMode.FALLBACK,
                    durationMs = elapsedMs(startedAt),
                    itemCount = parsed.episodes.size,
                    failureCategory = error.category,
                ),
            )
            parsed
        } catch (_: Exception) {
            val parsed = fallback.fetch(canonicalUrl, etag, lastModified, onEpisodeProgress)
            recordMetric(FeedParserMetric(FeedParserMode.FALLBACK, elapsedMs(startedAt), parsed.episodes.size))
            parsed
        }
    }

    private suspend fun fetchWithStreamingParser(
        feedUrl: String,
        etag: String?,
        lastModified: String?,
        onEpisodeProgress: (processed: Int, total: Int) -> Unit,
    ): PodcastFeed {
        Trace.beginSection("booth.feed.http.streaming")
        try {
            return client.newCall(
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
                if (contentType in setOf("text/html", "application/xhtml+xml")) {
                    throw NotAFeedResponseException()
                }
                val finalUrl = response.request.url.toString()
                val responseEtag = response.header("ETag") ?: etag
                val responseLastModified = response.header("Last-Modified") ?: lastModified
                if (response.code == 304) {
                    return@use PodcastFeed(
                        podcast = Podcast(
                            id = podcastId(finalUrl),
                            title = hostOf(finalUrl).orEmpty().ifBlank { finalUrl },
                            author = null,
                            feedUrl = finalUrl,
                            siteUrl = null,
                            descriptionHtml = null,
                            artworkUrl = null,
                        ),
                        episodes = emptyList(),
                        etag = responseEtag,
                        lastModified = responseLastModified,
                        notModified = true,
                    )
                }
                if (!response.isSuccessful) throw FeedHttpException(response.code)
                val responseBody = response.body
                var result: RssParseResult? = null
                var failure: FeedParseEvent.Failed? = null
                try {
                    responseBody.byteStream().use { input ->
                        parser.parse(input, finalUrl).collect { event ->
                            when (event) {
                                is FeedParseEvent.Completed -> result = event.result
                                is FeedParseEvent.EpisodeChunkAvailable -> Unit
                                is FeedParseEvent.FeedMetadataAvailable -> Unit
                                is FeedParseEvent.Warning -> Unit
                                is FeedParseEvent.Failed -> failure = event
                            }
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    throw StreamingParserFailureException(
                        finalUrl,
                        responseEtag,
                        responseLastModified,
                        FailureCategory.UNKNOWN,
                        error,
                    )
                }
                failure?.let {
                    throw StreamingParserFailureException(
                        finalUrl,
                        responseEtag,
                        responseLastModified,
                        it.category,
                        it.cause,
                    )
                }
                val parsed = result ?: throw StreamingParserFailureException(
                    finalUrl,
                    responseEtag,
                    responseLastModified,
                    FailureCategory.UNKNOWN,
                    FeedResponseException("Streaming parser produced no result"),
                )
                Trace.beginSection("booth.feed.map.streaming")
                try {
                    parsed.copy(etag = responseEtag, lastModified = responseLastModified)
                        .toPodcastFeed(onEpisodeProgress)
                } finally {
                    Trace.endSection()
                }
            }
        } finally {
            Trace.endSection()
        }
    }

    private fun recordMetric(metric: FeedParserMetric) {
        runCatching { metrics.record(metric) }
    }

    private fun elapsedMs(startedAt: Long): Long =
        ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(0L)
}

private class StreamingParserFailureException(
    val feedUrl: String,
    val etag: String?,
    val lastModified: String?,
    val category: FailureCategory,
    cause: Throwable,
) : IllegalStateException("Streaming parser failed", cause)
