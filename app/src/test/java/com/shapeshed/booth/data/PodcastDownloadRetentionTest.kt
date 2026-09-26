package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastDownloadRetentionTest {
    @Test
    fun protectsQueuedAndFavouriteDownloads() {
        val removable = episode(1)
        val queued = episode(2)
        val favourite = episode(3, favourite = true)

        assertEquals(
            listOf(removable),
            downloadsEligibleForDeletion(listOf(queued, favourite, removable), setOf(queued.id)),
        )
    }

    @Test
    fun deletesOldestEligibleDownloadsFirst() {
        val oldest = episode(1)
        val newest = episode(2)

        assertEquals(listOf(oldest, newest), downloadsEligibleForDeletion(listOf(newest, oldest), emptySet()))
    }

    @Test
    fun retainsUnplayedDownloadsWhenConfiguredToDeleteOnlyPlayedEpisodes() {
        val played = episode(1, completed = true)
        val unplayed = episode(2)

        assertEquals(
            listOf(played),
            downloadsEligibleForDeletion(
                listOf(unplayed, played),
                emptySet(),
                PodcastDeleteBeforeAutoDownload.PLAYED,
            ),
        )
    }

    @Test
    fun considersVideoOnlyEpisodesDownloaded() {
        val videoOnly = episode(1).copy(localUri = null, localVideoUri = "file:///episode-1.video")

        assertEquals(
            listOf(videoOnly),
            downloadsEligibleForDeletion(listOf(videoOnly), emptySet()),
        )
    }

    private fun episode(id: Long, favourite: Boolean = false, completed: Boolean = false) = EpisodeEntity(
        id = id, podcastId = 1, guid = id.toString(), title = id.toString(), descriptionHtml = null,
        audioUrl = "https://example.com/$id.mp3", mimeType = null, artworkUrl = null,
        publishedAtMillis = id, durationMs = null, positionMs = 0, completed = completed,
        localUri = "file:///episode-$id", inInbox = true, firstSeenAtMillis = null, favorite = favourite,
    )
}
