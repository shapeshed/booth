package com.shapeshed.booth.data

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadAssetTest {
    @Test
    fun validFileAcceptsUnknownOrMatchingSize() {
        val file = File.createTempFile("booth-download", ".mp3")
        try {
            file.writeBytes(ByteArray(4))
            assertTrue(isValidDownloadedFile(file, null))
            assertTrue(isValidDownloadedFile(file, 4L))
            assertTrue(isValidDownloadedFile(file, 0L))
        } finally {
            file.delete()
        }
    }

    @Test
    fun validFileRejectsEmptyOrMismatchedFile() {
        val file = File.createTempFile("booth-download", ".mp3")
        try {
            assertFalse(isValidDownloadedFile(file, null))
            file.writeBytes(ByteArray(4))
            assertFalse(isValidDownloadedFile(file, 3L))
        } finally {
            file.delete()
        }
    }

    @Test
    fun failedAssetDoesNotProduceProgress() {
        val failed = DownloadAssetEntity(
            episodeId = 42L,
            assetType = DownloadAssetType.AUDIO,
            downloadId = 7L,
            sourceUrl = "https://example.com/episode.mp3",
            destinationUri = "file:///tmp/episode.mp3",
            status = DownloadAssetStatus.FAILED,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )

        assertFalse(
            mergeDownloadProgress(
                assets = listOf(failed),
                liveProgress = mapOf(42L to DownloadProgress(0L, 0L, 1L)),
                nowElapsedMs = 1L,
            ).containsKey(42L),
        )
    }

}
