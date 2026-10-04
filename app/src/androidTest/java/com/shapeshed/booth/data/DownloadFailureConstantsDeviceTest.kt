package com.shapeshed.booth.data

import android.app.DownloadManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Ties the hard-coded reason values in [DownloadRetryPolicyTest] to the platform constants.
 *
 * The JVM tests cannot load `android.app.DownloadManager`, so they spell the ints out. That is only
 * safe while the spellings are right, which is what this asserts. Without it, a platform change would
 * leave the retry policy quietly testing the wrong reasons rather than failing.
 */
@RunWith(AndroidJUnit4::class)
class DownloadFailureConstantsDeviceTest {

    @Test
    fun platformErrorConstantsHaveNotMoved() {
        assertEquals(1000, DownloadManager.ERROR_UNKNOWN)
        assertEquals(1001, DownloadManager.ERROR_FILE_ERROR)
        assertEquals(1002, DownloadManager.ERROR_UNHANDLED_HTTP_CODE)
        assertEquals(1004, DownloadManager.ERROR_HTTP_DATA_ERROR)
        assertEquals(1005, DownloadManager.ERROR_TOO_MANY_REDIRECTS)
        assertEquals(1006, DownloadManager.ERROR_INSUFFICIENT_SPACE)
        assertEquals(1007, DownloadManager.ERROR_DEVICE_NOT_FOUND)
        assertEquals(1008, DownloadManager.ERROR_CANNOT_RESUME)
        assertEquals(1009, DownloadManager.ERROR_FILE_ALREADY_EXISTS)
    }

    /**
     * The two reasons the policy treats as transient. If the platform ever stopped using these for
     * resumable and mid-transfer HTTP failures, the retry would be aimed at the wrong errors.
     */
    @Test
    fun retriedReasonsAreTheResumableAndHttpDataErrors() {
        assertEquals(true, shouldRetryDownload(DownloadManager.ERROR_CANNOT_RESUME, 0))
        assertEquals(true, shouldRetryDownload(DownloadManager.ERROR_HTTP_DATA_ERROR, 0))
        assertEquals(false, shouldRetryDownload(DownloadManager.ERROR_INSUFFICIENT_SPACE, 0))
    }
}
