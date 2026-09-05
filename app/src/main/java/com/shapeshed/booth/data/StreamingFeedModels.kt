package com.shapeshed.booth.data

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

sealed interface FeedParseEvent {
    data class FeedMetadataAvailable(val feed: RssFeed) : FeedParseEvent
    data class EpisodeChunkAvailable(val entries: List<RssEntry>) : FeedParseEvent
    data class Warning(val message: String) : FeedParseEvent
    data class Completed(val result: RssParseResult) : FeedParseEvent
    data class Failed(val category: FailureCategory, val cause: Throwable) : FeedParseEvent
}

enum class FailureCategory { STRUCTURAL, UNSUPPORTED, LIMIT, IO, UNKNOWN }

enum class FeedFormat { RSS, RDF, ATOM }

enum class FeedParserMode { STREAMING, FALLBACK }

data class FeedParserMetric(
    val mode: FeedParserMode,
    val durationMs: Long,
    val itemCount: Int,
    val failureCategory: FailureCategory? = null,
)

fun interface FeedParserMetrics {
    fun record(metric: FeedParserMetric)
}

interface StreamingFeedParser {
    fun parse(input: InputStream, feedUrl: String): Flow<FeedParseEvent>
}

suspend fun StreamingFeedParser.parseCompletely(
    input: InputStream,
    feedUrl: String,
): RssParseResult {
    var failure: FeedParseEvent.Failed? = null
    var completed: RssParseResult? = null
    parse(input, feedUrl).collect { event ->
        when (event) {
            is FeedParseEvent.Completed -> completed = event.result
            is FeedParseEvent.Failed -> failure = event
            else -> Unit
        }
    }
    failure?.let { throw FeedResponseException("Streaming parser failed (${it.category})", it.cause) }
    return completed ?: throw FeedResponseException("Streaming parser produced no result")
}
