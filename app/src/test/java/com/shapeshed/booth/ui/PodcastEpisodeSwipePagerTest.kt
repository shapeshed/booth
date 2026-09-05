package com.shapeshed.booth.ui

import com.shapeshed.booth.data.EpisodeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastEpisodeSwipePagerTest {
    @Test
    fun selectedEpisodeUsesItsSequencePosition() {
        val episodes = listOf(episode(1), episode(2), episode(3))

        assertEquals(1, episodeSwipeIndex(episodes, 2))
    }

    @Test
    fun missingEpisodeStartsAtFirstPage() {
        assertEquals(0, episodeSwipeIndex(listOf(episode(1)), 99))
    }

    private fun episode(id: Long) = EpisodeEntity(
        id = id,
        podcastId = 1,
        guid = "guid-$id",
        title = "Episode $id",
        descriptionHtml = null,
        audioUrl = "https://example.com/$id.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = id,
        durationMs = null,
        positionMs = 0,
        completed = false,
        localUri = null,
        inInbox = true,
        firstSeenAtMillis = id,
    )
}
