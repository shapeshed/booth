package com.shapeshed.booth.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayedDownloadCleanupWorkerTest {
    @Test
    fun removesOnlyPlayedEpisodesWithLocalMedia() {
        assertTrue(shouldRemovePlayedDownload(episode(completed = true, localUri = "file:///audio")))
        assertTrue(shouldRemovePlayedDownload(episode(completed = true, localVideoUri = "file:///video")))
        assertFalse(shouldRemovePlayedDownload(episode(completed = false, localUri = "file:///audio")))
        assertFalse(shouldRemovePlayedDownload(episode(completed = true)))
    }

    private fun episode(
        completed: Boolean,
        localUri: String? = null,
        localVideoUri: String? = null,
    ) = EpisodeEntity(
        id = 1,
        podcastId = 1,
        guid = "episode-1",
        title = "Episode",
        descriptionHtml = null,
        audioUrl = "https://example.com/episode.mp3",
        mimeType = null,
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = null,
        positionMs = 0L,
        completed = completed,
        localUri = localUri,
        localVideoUri = localVideoUri,
        inInbox = true,
        firstSeenAtMillis = null,
    )
}
