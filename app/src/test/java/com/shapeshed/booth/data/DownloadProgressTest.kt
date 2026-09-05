package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadProgressTest {
    @Test
    fun liveProgressWinsWhenItIsAheadOfDurableState() {
        val asset = DownloadAssetEntity(
            episodeId = 7L,
            assetType = DownloadAssetType.AUDIO,
            downloadId = 1L,
            sourceUrl = "https://example.com/episode.mp3",
            destinationUri = "file:///tmp/episode.mp3",
            status = DownloadAssetStatus.DOWNLOADING,
            bytesDownloaded = 10L,
            totalBytes = 100L,
            createdAtMillis = 1L,
            updatedAtMillis = 2L,
        )

        val result = mergeDownloadProgress(
            assets = listOf(asset),
            liveProgress = mapOf(7L to DownloadProgress(50L, 100L, 3L)),
            nowElapsedMs = 4L,
        )

        assertEquals(50L, result.getValue(7L).bytesDownloaded)
        assertEquals(3L, result.getValue(7L).startedAtElapsedMs)
        assertEquals(false, result.getValue(7L).completed)
    }

    @Test
    fun completedAudioIsPreferredOverCompletedVideo() {
        val video = DownloadAssetEntity(
            episodeId = 9L,
            assetType = DownloadAssetType.VIDEO,
            downloadId = 2L,
            sourceUrl = "https://example.com/episode.mp4",
            destinationUri = "file:///tmp/episode.mp4",
            status = DownloadAssetStatus.COMPLETED,
            bytesDownloaded = 200L,
            totalBytes = 200L,
            createdAtMillis = 1L,
            updatedAtMillis = 5L,
        )
        val audio = video.copy(
            assetType = DownloadAssetType.AUDIO,
            destinationUri = "file:///tmp/episode.mp3",
            bytesDownloaded = 100L,
            totalBytes = 100L,
            updatedAtMillis = 2L,
        )

        val result = mergeDownloadProgress(listOf(video, audio), emptyMap(), nowElapsedMs = 6L)

        assertEquals(100L, result.getValue(9L).bytesDownloaded)
        assertEquals(true, result.getValue(9L).completed)
    }
}
