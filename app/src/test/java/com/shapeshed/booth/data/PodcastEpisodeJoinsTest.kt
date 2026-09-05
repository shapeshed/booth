package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastEpisodeJoinsTest {
    @Test
    fun orderedQueueEpisodesFollowsQueueOrderAndSkipsMissingEpisodes() {
        val first = episode(1L)
        val third = episode(3L)

        val ordered = orderedQueueEpisodes(
            entries = listOf(QueueEntity(3L, 0), QueueEntity(2L, 1), QueueEntity(1L, 2)),
            episodesById = mapOf(first.id to first, third.id to third),
        )

        assertEquals(listOf(third.id, first.id), ordered.map(EpisodeEntity::id))
    }

    @Test
    fun downloadEpisodeIdsAreDistinctAndKeepFirstSeenOrder() {
        val assets = listOf(
            asset(10L, DownloadAssetType.AUDIO, 1L),
            asset(10L, DownloadAssetType.VIDEO, 2L),
            asset(20L, DownloadAssetType.AUDIO, 3L),
        )

        assertEquals(listOf(10L, 20L), downloadEpisodeIds(assets))
    }

    private fun asset(episodeId: Long, type: DownloadAssetType, downloadId: Long) = DownloadAssetEntity(
        episodeId = episodeId,
        assetType = type,
        downloadId = downloadId,
        sourceUrl = "https://example.com/$episodeId",
        destinationUri = "file:///tmp/$episodeId",
        status = DownloadAssetStatus.QUEUED,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    private fun episode(id: Long) = EpisodeEntity(
        id = id,
        podcastId = 1L,
        guid = "episode-$id",
        title = "Episode $id",
        descriptionHtml = null,
        audioUrl = "https://example.com/$id.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = null,
        positionMs = 0L,
        completed = false,
        localUri = null,
        inInbox = false,
        firstSeenAtMillis = null,
    )
}
