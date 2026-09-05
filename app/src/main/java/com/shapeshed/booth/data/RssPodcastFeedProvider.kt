package com.shapeshed.booth.data

import android.os.Trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Adapts the existing RSS/Atom parser to the podcast domain. */
class RssPodcastFeedProvider(
    private val parser: FeedParser,
) : PodcastFeedProvider {
    override suspend fun fetch(
        feedUrl: String,
        etag: String?,
        lastModified: String?,
        onEpisodeProgress: (processed: Int, total: Int) -> Unit,
    ): PodcastFeed = withContext(Dispatchers.IO) {
        val canonicalUrl = canonicalFeedUrl(feedUrl)
        val parsed = parser.fetch(canonicalUrl, etag, lastModified)
        Trace.beginSection("booth.feed.map")
        try {
            parsed.toPodcastFeed(onEpisodeProgress)
        } finally {
            Trace.endSection()
        }
    }
}
