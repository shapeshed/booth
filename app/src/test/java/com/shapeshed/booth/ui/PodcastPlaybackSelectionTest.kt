package com.shapeshed.booth.ui

import com.shapeshed.booth.data.EpisodeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastPlaybackSelectionTest {
    @Test
    fun replayingACompletedEpisodeStartsAtTheBeginning() {
        val episode = episode(positionMs = 1_234L, completed = true)

        assertEquals(0L, playbackStartPositionMs(episode))
    }

    @Test
    fun replayingAnIncompleteEpisodeKeepsItsSavedPosition() {
        val episode = episode(positionMs = 1_234L, completed = false)

        assertEquals(1_234L, playbackStartPositionMs(episode))
    }

    private fun episode(positionMs: Long, completed: Boolean) = EpisodeEntity(
        id = 1L,
        podcastId = 2L,
        guid = "episode",
        title = "Episode",
        descriptionHtml = null,
        audioUrl = "https://example.com/episode.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = 10_000L,
        positionMs = positionMs,
        completed = completed,
        localUri = null,
        inInbox = true,
        firstSeenAtMillis = null,
    )
}
