package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the retry budget, which is the part of the failure story a user can actually see.
 *
 * The failure dialog quotes [MAX_DOWNLOAD_RETRIES] back to the user, so the number in the copy and the
 * number the guard enforces have to be the same constant. These pin both, and the reset that a manual
 * retry depends on.
 *
 * The reason ints are spelled out below because `shouldRetryDownload` and
 * `downloadManagerFailureMessage` take the platform values and these are JVM tests, where
 * `android.app.DownloadManager` is a stub. `DownloadFailureConstantsDeviceTest` asserts the same
 * values still match the platform class on a device, which is what keeps this from drifting.
 */
class DownloadRetryPolicyTest {

    @Test
    fun `a transient failure is retried while budget remains`() {
        assertTrue(shouldRetryDownload(ERROR_CANNOT_RESUME, 0))
        assertTrue(shouldRetryDownload(ERROR_HTTP_DATA_ERROR, 0))
    }

    @Test
    fun `the budget is exhausted exactly at the cap`() {
        val lastAllowed = MAX_DOWNLOAD_RETRIES - 1
        assertTrue(
            "retry $lastAllowed should still be allowed",
            shouldRetryDownload(ERROR_HTTP_DATA_ERROR, lastAllowed),
        )
        assertFalse(
            "retry $MAX_DOWNLOAD_RETRIES must be refused",
            shouldRetryDownload(ERROR_HTTP_DATA_ERROR, MAX_DOWNLOAD_RETRIES),
        )
        assertFalse(shouldRetryDownload(ERROR_HTTP_DATA_ERROR, MAX_DOWNLOAD_RETRIES + 5))
    }

    @Test
    fun `a failure that backoff cannot fix is never retried`() {
        listOf(
            ERROR_INSUFFICIENT_SPACE,
            ERROR_UNHANDLED_HTTP_CODE,
            ERROR_TOO_MANY_REDIRECTS,
            ERROR_FILE_ERROR,
            ERROR_DEVICE_NOT_FOUND,
        ).forEach { reason ->
            assertFalse("reason $reason should not be retried", shouldRetryDownload(reason, 0))
        }
    }

    /**
     * The counter the guard reads is what an explicit retry resets. That reset is a database write, so
     * it is pinned against the real Room database in `DownloadRetryResetDeviceTest` rather than
     * restated here as a predicate call. What this covers is only that the guard itself flips at the
     * boundary, which is the arithmetic the reset depends on.
     */
    @Test
    fun `the guard blocks at the cap and allows below it`() {
        assertFalse(shouldRetryDownload(ERROR_HTTP_DATA_ERROR, MAX_DOWNLOAD_RETRIES))
        assertTrue(shouldRetryDownload(ERROR_HTTP_DATA_ERROR, MAX_DOWNLOAD_RETRIES - 1))
        assertTrue(shouldRetryDownload(ERROR_HTTP_DATA_ERROR, 0))
    }

    /**
     * The enqueue layer has its own, smaller budget. Capping it is what stops a URL the platform
     * refuses outright from rescheduling for hours.
     */
    @Test
    fun `the enqueue budget is separate and capped`() {
        assertEquals(EnqueueFailure.RETRY, classifyEnqueueFailure(0))
        assertEquals(EnqueueFailure.RETRY, classifyEnqueueFailure(MAX_ENQUEUE_ATTEMPTS - 1))
        assertEquals(EnqueueFailure.GIVE_UP, classifyEnqueueFailure(MAX_ENQUEUE_ATTEMPTS))
        assertEquals(EnqueueFailure.GIVE_UP, classifyEnqueueFailure(MAX_ENQUEUE_ATTEMPTS + 10))
    }

    /**
     * Every reason the UI can be handed has wording a person can act on, so the dialog never shows a
     * bare number for a reason the app recognises.
     */
    @Test
    fun `recognised failures have human wording and no raw codes`() {
        ALL_REASONS.forEach { reason ->
            val message = downloadManagerFailureMessage(reason)
            assertTrue("reason $reason produced no message", message.isNotBlank())
            assertFalse("reason $reason leaked a raw code: $message", message.contains("code"))
            assertTrue("reason $reason read as an error code: $message", message.contains('.'))
        }
    }

    @Test
    fun `an unrecognised reason still produces something readable`() {
        val message = downloadManagerFailureMessage(987_654)
        assertTrue(message.isNotBlank())
    }

    /**
     * An HTTP 404 is the one failure where retrying cannot help.
     *
     * DownloadManager reports ERROR_UNHANDLED_HTTP_CODE with COLUMN_REASON set to the HTTP status
     * itself, so a 404 arrives as the bare int 404 rather than as an ERROR_* constant. Observed on a
     * device: an FT News Briefing enclosure that had been pulled reported "(code 404)". Without this
     * branch it fell through to the raw-code fallback and the user was told to retry a URL that no
     * longer exists.
     */
    @Test
    fun `a 404 is named as gone rather than shown as a raw code`() {
        val message = downloadManagerFailureMessage(404)
        assertEquals(DOWNLOAD_UNAVAILABLE_MESSAGE, message)
        assertFalse("the raw code leaked to the user", message.contains("404"))
        assertTrue(isDownloadPermanentlyUnavailable(message))
    }

    @Test
    fun `a 410 is also gone`() {
        assertEquals(DOWNLOAD_UNAVAILABLE_MESSAGE, downloadManagerFailureMessage(410))
        assertTrue(isDownloadPermanentlyUnavailable(downloadManagerFailureMessage(410)))
    }

    /**
     * The predicate has to recognise the message it wrote, because the dialog only ever sees the
     * stored string, never the platform reason.
     */
    @Test
    fun `only the gone message counts as permanent`() {
        assertFalse(isDownloadPermanentlyUnavailable(null))
        assertFalse(isDownloadPermanentlyUnavailable(""))
        assertFalse(isDownloadPermanentlyUnavailable(DOWNLOAD_FAILED_TO_START_MESSAGE))
        assertFalse(
            "a retryable failure must not be mistaken for a permanent one",
            isDownloadPermanentlyUnavailable(downloadManagerFailureMessage(ERROR_HTTP_DATA_ERROR)),
        )
        assertFalse(
            "an unrecognised code is not proof that the file is gone",
            isDownloadPermanentlyUnavailable(downloadManagerFailureMessage(987_654)),
        )
    }

    /** A gone enclosure must not be advertised as retryable either. */
    @Test
    fun `a gone enclosure is not a retryable reason`() {
        assertFalse(shouldRetryDownload(404, 0))
        assertFalse(shouldRetryDownload(410, 0))
    }

    private companion object {
        // Values of android.app.DownloadManager, asserted against the real class on device by
        // DownloadFailureConstantsDeviceTest.
        const val ERROR_UNKNOWN = 1000
        const val ERROR_FILE_ERROR = 1001
        const val ERROR_UNHANDLED_HTTP_CODE = 1002
        const val ERROR_HTTP_DATA_ERROR = 1004
        const val ERROR_TOO_MANY_REDIRECTS = 1005
        const val ERROR_INSUFFICIENT_SPACE = 1006
        const val ERROR_DEVICE_NOT_FOUND = 1007
        const val ERROR_CANNOT_RESUME = 1008
        const val ERROR_FILE_ALREADY_EXISTS = 1009

        val ALL_REASONS = listOf(
            ERROR_UNKNOWN,
            ERROR_FILE_ERROR,
            ERROR_UNHANDLED_HTTP_CODE,
            ERROR_HTTP_DATA_ERROR,
            ERROR_TOO_MANY_REDIRECTS,
            ERROR_INSUFFICIENT_SPACE,
            ERROR_DEVICE_NOT_FOUND,
            ERROR_CANNOT_RESUME,
            ERROR_FILE_ALREADY_EXISTS,
        )
    }
}
