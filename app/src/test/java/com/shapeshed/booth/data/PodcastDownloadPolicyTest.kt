package com.shapeshed.booth.data

import android.app.DownloadManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastDownloadPolicyTest {
    @Test
    fun retriesTransientDownloadFailuresUntilTheLimit() {
        assertTrue(shouldRetryDownload(DownloadManager.ERROR_CANNOT_RESUME, 0))
        assertTrue(shouldRetryDownload(DownloadManager.ERROR_HTTP_DATA_ERROR, 2))
        assertFalse(shouldRetryDownload(DownloadManager.ERROR_HTTP_DATA_ERROR, 3))
    }

    @Test
    fun doesNotRetryPermanentHttpFailures() {
        assertFalse(shouldRetryDownload(DownloadManager.ERROR_UNHANDLED_HTTP_CODE, 0))
    }
}
