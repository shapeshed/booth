package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the one rule that decides what badge every list view draws.
 *
 * These exist because the three list views used to derive this separately and disagreed: a failed
 * download drew an icon inline on Downloads and nothing at all in Up next and Inbox. A test per
 * view would not have caught that, because each view was individually self-consistent. The property
 * under test is the shared resolver, so that is what is asserted.
 */
class DownloadBadgeTest {

    private fun asset(status: DownloadAssetStatus, errorMessage: String? = null, retryCount: Int = 0) =
        DownloadAssetEntity(
            episodeId = 1L,
            assetType = DownloadAssetType.AUDIO,
            downloadId = 10L,
            sourceUrl = "https://example.com/audio.mp3",
            destinationUri = "file:///data/audio.mp3",
            status = status,
            errorMessage = errorMessage,
            retryCount = retryCount,
            createdAtMillis = 0L,
            updatedAtMillis = 0L,
        )

    private fun progress(
        bytesDownloaded: Long = 0L,
        totalBytes: Long = 100L,
        completed: Boolean = false,
        waitingForWifi: Boolean = false,
    ) = DownloadProgress(
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
        startedAtElapsedMs = 0L,
        completed = completed,
        waitingForWifi = waitingForWifi,
    )

    @Test
    fun `no row and no progress draws nothing`() {
        assertEquals(DownloadBadge.NONE, downloadBadge(asset = null, progress = null, isDownloaded = false))
    }

    @Test
    fun `every durable status maps to its own badge`() {
        val expected = mapOf(
            DownloadAssetStatus.QUEUED to DownloadBadge.DOWNLOADING,
            DownloadAssetStatus.WAITING_FOR_WIFI to DownloadBadge.WAITING_FOR_WIFI,
            DownloadAssetStatus.DOWNLOADING to DownloadBadge.DOWNLOADING,
            DownloadAssetStatus.RETRYING to DownloadBadge.RETRYING,
            DownloadAssetStatus.COMPLETED to DownloadBadge.DOWNLOADED,
            DownloadAssetStatus.FAILED to DownloadBadge.FAILED,
            DownloadAssetStatus.CANCELLED to DownloadBadge.CANCELLED,
        )

        DownloadAssetStatus.entries.forEach { status ->
            assertEquals(
                "status $status",
                expected.getValue(status),
                downloadBadge(asset(status), progress = null, isDownloaded = false),
            )
        }
    }

    /**
     * The gap that made a retrying download invisible: `downloadStatusLabel` had a branch for it
     * that no icon could ever reach, because the icon returned early for anything that was not
     * FAILED, WAITING_FOR_WIFI or CANCELLED.
     */
    @Test
    fun `retrying is drawn rather than skipped`() {
        assertEquals(
            DownloadBadge.RETRYING,
            downloadBadge(asset(DownloadAssetStatus.RETRYING), progress = null, isDownloaded = false),
        )
    }

    /**
     * A failed row can still be carrying a stale fraction, because the progress store is cleared on
     * a separate path from the status write. The terminal state has to win or the row reads as
     * still going when it has already given up.
     */
    @Test
    fun `a failed row reads as failed even while progress says otherwise`() {
        assertEquals(
            DownloadBadge.FAILED,
            downloadBadge(
                asset = asset(DownloadAssetStatus.FAILED),
                progress = progress(bytesDownloaded = 50L),
                isDownloaded = false,
            ),
        )
    }

    @Test
    fun `a cancelled row outranks stale progress too`() {
        assertEquals(
            DownloadBadge.CANCELLED,
            downloadBadge(
                asset = asset(DownloadAssetStatus.CANCELLED),
                progress = progress(bytesDownloaded = 50L),
                isDownloaded = false,
            ),
        )
    }

    /**
     * Bytes on disk are the strongest fact available. If the file is there and playable, the episode
     * is available offline whatever a stale status column claims.
     */
    @Test
    fun `a playable file outranks a stale failed status`() {
        assertEquals(
            DownloadBadge.DOWNLOADED,
            downloadBadge(
                asset = asset(DownloadAssetStatus.FAILED),
                progress = null,
                isDownloaded = true,
            ),
        )
    }

    @Test
    fun `a playable file outranks an in-flight status`() {
        assertEquals(
            DownloadBadge.DOWNLOADED,
            downloadBadge(
                asset = asset(DownloadAssetStatus.DOWNLOADING),
                progress = progress(bytesDownloaded = 10L),
                isDownloaded = true,
            ),
        )
    }

    /**
     * The pre-enqueue window: the progress store knows about the transfer before the database row
     * does, and that is the only case where progress gets a say.
     */
    @Test
    fun `progress alone decides when there is no row yet`() {
        assertEquals(
            DownloadBadge.DOWNLOADING,
            downloadBadge(asset = null, progress = progress(bytesDownloaded = 50L), isDownloaded = false),
        )
        assertEquals(
            DownloadBadge.WAITING_FOR_WIFI,
            downloadBadge(asset = null, progress = progress(waitingForWifi = true), isDownloaded = false),
        )
        assertEquals(
            DownloadBadge.DOWNLOADED,
            downloadBadge(asset = null, progress = progress(completed = true), isDownloaded = false),
        )
    }

    @Test
    fun `progress that has finished its fraction is no longer active`() {
        assertEquals(
            DownloadBadge.NONE,
            downloadBadge(
                asset = null,
                progress = progress(bytesDownloaded = 100L, totalBytes = 100L),
                isDownloaded = false,
            ),
        )
    }

    /**
     * A queued transfer has no total size yet. It must still read as active, or the row looks idle
     * between the tap and the first progress report.
     */
    @Test
    fun `a queued row with no known size is drawn as active`() {
        assertEquals(
            DownloadBadge.DOWNLOADING,
            downloadBadge(
                asset = asset(DownloadAssetStatus.QUEUED),
                progress = progress(bytesDownloaded = 0L, totalBytes = 0L),
                isDownloaded = false,
            ),
        )
    }

    /**
     * The durable row is the authority on waiting for Wi-Fi, because that state comes from
     * DownloadManager's own paused reasons rather than from anything Booth infers.
     */
    @Test
    fun `a waiting row is not overridden by active-looking progress`() {
        assertEquals(
            DownloadBadge.WAITING_FOR_WIFI,
            downloadBadge(
                asset = asset(DownloadAssetStatus.WAITING_FOR_WIFI),
                progress = progress(bytesDownloaded = 50L),
                isDownloaded = false,
            ),
        )
    }

    @Test
    fun `only failed and cancelled offer the failure details`() {
        assertEquals(true, DownloadBadge.FAILED.explainsFailure())
        assertEquals(true, DownloadBadge.CANCELLED.explainsFailure())

        listOf(
            DownloadBadge.NONE,
            DownloadBadge.DOWNLOADED,
            DownloadBadge.DOWNLOADING,
            DownloadBadge.RETRYING,
            DownloadBadge.WAITING_FOR_WIFI,
        ).forEach { badge ->
            assertEquals("badge $badge", false, badge.explainsFailure())
        }
    }
}
