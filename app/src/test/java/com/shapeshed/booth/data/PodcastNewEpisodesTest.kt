package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastNewEpisodesTest {
    @Test
    fun selectsOnlyEpisodesAddedAfterTheBaseline() {
        val existing = episode(guid = "existing")
        val newEpisode = episode(guid = "new")

        assertEquals(
            listOf(newEpisode),
            newEpisodesSince(
                episodes = listOf(existing, newEpisode),
                existingEpisodeIdentities = setOf(existing.guid to existing.audioUrl),
            ),
        )
    }

    @Test
    fun changedGuidDoesNotMakeAnImportedEpisodeNew() {
        val imported = episode(guid = "old-guid", audioUrl = "https://example.com/episode.mp3")
        val refreshed = episode(guid = "new-guid", audioUrl = imported.audioUrl)

        assertEquals(
            emptyList<EpisodeEntity>(),
            newEpisodesSince(
                episodes = listOf(refreshed),
                existingEpisodeIdentities = setOf(imported.guid to imported.audioUrl),
            ),
        )
    }

    @Test
    fun changedEnclosureDoesNotMakeAnEpisodeWithTheSameGuidNew() {
        val imported = episode(guid = "stable-guid", audioUrl = "https://old.example.com/episode.mp3")
        val refreshed = episode(guid = imported.guid, audioUrl = "https://new.example.com/episode.mp3")

        assertEquals(
            emptyList<EpisodeEntity>(),
            newEpisodesSince(
                episodes = listOf(refreshed),
                existingEpisodeIdentities = setOf(imported.guid to imported.audioUrl),
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
