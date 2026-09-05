package com.shapeshed.booth.ui

import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastEpisodeActionTest {
    @Test
    fun subscribedActionCombinesPodcastAndDownloadState() {
        val podcast = podcast()
        val episode = episode()

        val action = createSubscribedEpisodeAction(
            episode = episode,
            podcastsById = mapOf(podcast.id to podcast),
            downloadProgress = mapOf(
                episode.id to DownloadProgress(
                    bytesDownloaded = 64L,
                    totalBytes = 128L,
                    startedAtElapsedMs = 1L,
                    completed = true,
                ),
            ),
            queueEpisodeIds = listOf(episode.id),
        )

        assertEquals("Example podcast", action.podcastTitle)
        assertEquals("https://example.com/episode", action.linkUrl)
        assertTrue(action.isDownloaded)
        assertEquals(128L, action.downloadSizeBytes)
        assertTrue(action.isInQueue)
        assertTrue(action.hasPlaybackPosition)
    }

    private fun podcast() = PodcastEntity(
        id = 7L,
        title = "Example podcast",
        author = null,
        feedUrl = "https://example.com/feed.xml",
        siteUrl = "https://example.com",
        descriptionHtml = null,
        artworkUrl = null,
        subscribedAtMillis = 0L,
        lastRefreshMillis = null,
    )

    private fun episode() = EpisodeEntity(
        id = 9L,
        podcastId = 7L,
        guid = "episode-9",
        title = "Example episode",
        descriptionHtml = null,
        audioUrl = "https://example.com/episode.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = 100L,
        durationMs = 1_000L,
        positionMs = 10L,
        completed = false,
        localUri = null,
        inInbox = false,
        firstSeenAtMillis = 100L,
        linkUrl = "https://example.com/episode",
    )
}
