package com.shapeshed.booth.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The retry reset, against the real Room database.
 *
 * This is the piece the user-visible fix depends on and it was previously untested: `retryDownload`
 * calls `resetDownloadRetryCount`, and without the counter actually reaching zero a Retry tap on an
 * exhausted row would be refused by `shouldRetryDownload` on its next failure, making the button look
 * broken for exactly the downloads a user most wants to retry. A pure test of `shouldRetryDownload`
 * cannot show that, because it never touches the stored counter.
 */
@RunWith(AndroidJUnit4::class)
class DownloadRetryResetDeviceTest {

    private lateinit var database: PodcastDatabase
    private lateinit var dao: DownloadAssetDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PodcastDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.downloadAssetDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun repository() = PodcastRepository(
        feedProvider = object : PodcastFeedProvider {
            override suspend fun fetch(
                feedUrl: String,
                etag: String?,
                lastModified: String?,
                onEpisodeProgress: (processed: Int, total: Int) -> Unit,
            ): PodcastFeed = error("network is not used by this test")
        },
        dao = database.podcastDao(),
        downloadDao = dao,
    )

    private fun asset(
        status: DownloadAssetStatus = DownloadAssetStatus.FAILED,
        retryCount: Int = 0,
        errorMessage: String? = null,
    ) = DownloadAssetEntity(
        episodeId = 1L,
        assetType = DownloadAssetType.AUDIO,
        downloadId = 1L,
        sourceUrl = "https://example.com/1.mp3",
        destinationUri = "file:///tmp/1.mp3",
        status = status,
        bytesDownloaded = 100L,
        totalBytes = 200L,
        errorMessage = errorMessage,
        retryCount = retryCount,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    @Test
    fun resettingClearsTheRetryCountSoTheBudgetIsWholeAgain() = runBlocking {
        dao.upsert(asset(retryCount = MAX_DOWNLOAD_RETRIES, errorMessage = "The download could not resume."))

        val before = dao.find(1L, DownloadAssetType.AUDIO)
        assertEquals(MAX_DOWNLOAD_RETRIES, before?.retryCount)
        // The precondition that made a naive Retry a no-op.
        assertTrue(!shouldRetryDownload(1008, before!!.retryCount))

        dao.resetRetryCount(1L, DownloadAssetType.AUDIO, updatedAtMillis = 5L)

        val after = requireNotNull(dao.find(1L, DownloadAssetType.AUDIO))
        assertEquals("retry budget was not restored", 0, after.retryCount)
        assertTrue(
            "an explicit retry must be able to fail and be retried again",
            shouldRetryDownload(1008, after.retryCount),
        )
    }

    /**
     * The stale reason must go too, or the dialog keeps showing the previous failure while the new
     * attempt is running.
     */
    @Test
    fun resettingClearsTheRecordedReason() = runBlocking {
        dao.upsert(asset(errorMessage = "The server connection failed while downloading."))

        dao.resetRetryCount(1L, DownloadAssetType.AUDIO, updatedAtMillis = 5L)

        assertNull(requireNotNull(dao.find(1L, DownloadAssetType.AUDIO)).errorMessage)
    }

    /**
     * A reset is not a status change. The worker still has to drive the row out of FAILED, and
     * silently marking it COMPLETED here would claim a file that does not exist.
     */
    @Test
    fun resettingLeavesTheStatusAlone() = runBlocking {
        dao.upsert(asset(status = DownloadAssetStatus.FAILED, retryCount = MAX_DOWNLOAD_RETRIES))

        dao.resetRetryCount(1L, DownloadAssetType.AUDIO, updatedAtMillis = 5L)

        assertEquals(
            DownloadAssetStatus.FAILED,
            requireNotNull(dao.find(1L, DownloadAssetType.AUDIO)).status,
        )
    }

    @Test
    fun resettingOnlyTouchesTheRequestedAssetType() = runBlocking {
        dao.upsert(asset(retryCount = MAX_DOWNLOAD_RETRIES, errorMessage = "audio failed"))
        dao.upsert(
            asset(retryCount = MAX_DOWNLOAD_RETRIES, errorMessage = "video failed")
                .copy(assetType = DownloadAssetType.VIDEO, downloadId = 2L),
        )

        dao.resetRetryCount(1L, DownloadAssetType.AUDIO, updatedAtMillis = 5L)

        assertEquals(0, requireNotNull(dao.find(1L, DownloadAssetType.AUDIO)).retryCount)
        assertEquals(
            "the video asset's budget was reset too",
            MAX_DOWNLOAD_RETRIES,
            requireNotNull(dao.find(1L, DownloadAssetType.VIDEO)).retryCount,
        )
    }

    /**
     * What `markRetrying` does, for contrast: it spends a retry, which is the thing the reset undoes.
     */
    @Test
    fun markingRetryingSpendsABudgetAndResettingRestoresIt() = runBlocking {
        dao.upsert(asset(retryCount = 0, errorMessage = null))

        dao.markRetrying(
            episodeId = 1L,
            assetType = DownloadAssetType.AUDIO,
            status = DownloadAssetStatus.RETRYING,
            errorMessage = "The download could not resume.",
            updatedAtMillis = 5L,
        )
        val spent = requireNotNull(dao.find(1L, DownloadAssetType.AUDIO))
        assertEquals(1, spent.retryCount)
        assertEquals(DownloadAssetStatus.RETRYING, spent.status)

        dao.resetRetryCount(1L, DownloadAssetType.AUDIO, updatedAtMillis = 6L)
        assertEquals(0, requireNotNull(dao.find(1L, DownloadAssetType.AUDIO)).retryCount)
    }

    /**
     * The worker's give-up path must actually write FAILED.
     *
     * It previously only cleared the progress store, stranding the row at QUEUED with nothing driving
     * it: no spinner, no failure, and nothing for the badge or the dialog to show. This is the
     * assertion that would have caught it, and it is deliberately about the status rather than about
     * the function existing.
     */
    @Test
    fun aPermanentEnqueueFailureIsWrittenAsFailed() = runBlocking {
        dao.upsert(asset(status = DownloadAssetStatus.QUEUED, retryCount = 0, errorMessage = null))
        val repository = repository()

        repository.markDownloadFailedPermanently(
            episodeId = 1L,
            assetType = DownloadAssetType.AUDIO,
            reason = "Download URL must use HTTP or HTTPS",
        )

        val row = requireNotNull(dao.find(1L, DownloadAssetType.AUDIO))
        assertEquals(DownloadAssetStatus.FAILED, row.status)
        assertEquals("the reason was not kept", "Download URL must use HTTP or HTTPS", row.errorMessage)
    }

    /**
     * The reason must survive. An earlier version called `resetRetryCount` after writing it, and that
     * query also clears `errorMessage`, so the dialog would have reported "the reason was not
     * recorded" for every permanent failure.
     */
    @Test
    fun thePermanentFailureReasonIsNotWipedByTheWrite() = runBlocking {
        dao.upsert(asset(status = DownloadAssetStatus.QUEUED))

        repository().markDownloadFailedPermanently(1L, DownloadAssetType.AUDIO, "the specific reason")

        assertEquals(
            "the recorded reason was cleared as a side effect",
            "the specific reason",
            requireNotNull(dao.find(1L, DownloadAssetType.AUDIO)).errorMessage,
        )
    }

    @Test
    fun aPermanentFailureWithNoReasonStillSaysSomething() = runBlocking {
        dao.upsert(asset(status = DownloadAssetStatus.QUEUED))

        repository().markDownloadFailedPermanently(1L, DownloadAssetType.AUDIO, reason = null)

        assertEquals(
            DOWNLOAD_FAILED_TO_START_MESSAGE,
            requireNotNull(dao.find(1L, DownloadAssetType.AUDIO)).errorMessage,
        )
    }

    /**
     * The reset must reach the same row the retry guard reads, through the repository the ViewModel
     * actually uses rather than the DAO directly.
     */
    @Test
    fun theRepositoryResetReachesTheStoredRow() = runBlocking {
        dao.upsert(asset(retryCount = MAX_DOWNLOAD_RETRIES, errorMessage = "boom"))

        repository().resetDownloadRetryCount(1L, DownloadAssetType.AUDIO)

        val row = requireNotNull(dao.find(1L, DownloadAssetType.AUDIO))
        assertEquals(0, row.retryCount)
        assertNull(row.errorMessage)
    }
}
