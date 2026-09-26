package com.shapeshed.booth.ui

import com.shapeshed.booth.data.EpisodeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastRefreshAutoActionTest {
    @Test
    fun firstSuccessfulImportRequestsSubscriptionsDestination() {
        assertEquals(true, importHasSavedPodcast(imported = 1))
        assertEquals(false, importHasSavedPodcast(imported = 0))
    }

    @Test
    fun refreshSelectsOnlyEpisodesThatAreNewForQueueAndDownload() {
        val existing = episode(guid = "existing")
        val newEpisode = episode(guid = "new")

        assertEquals(
            listOf(newEpisode),
            newEpisodesSince(
                listOf(existing, newEpisode),
                setOf(existing.guid to existing.audioUrl),
            ),
        )
    }

    @Test
    fun changedGuidDoesNotMakeExistingEnclosureNew() {
        val existing = episode(guid = "old-guid", audioUrl = "https://example.com/episode.mp3")
        val refreshed = episode(guid = "new-guid", audioUrl = existing.audioUrl)

        assertEquals(
            emptyList<EpisodeEntity>(),
            newEpisodesSince(
                listOf(refreshed),
                setOf(existing.guid to existing.audioUrl),
            ),
        )
    }

    private fun episode(guid: String, audioUrl: String = "https://example.com/$guid.mp3") = EpisodeEntity(
        id = guid.hashCode().toLong(),
        podcastId = 1L,
        guid = guid,
        title = guid,
        descriptionHtml = null,
        audioUrl = audioUrl,
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = null,
        positionMs = 0L,
        completed = false,
        localUri = null,
        inInbox = true,
        firstSeenAtMillis = null,
    )
}
