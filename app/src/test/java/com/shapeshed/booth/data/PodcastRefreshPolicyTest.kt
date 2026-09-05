package com.shapeshed.booth.data

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastRefreshPolicyTest {
    @Test
    fun retriesTransientHttpFailures() {
        assertTrue(FeedHttpException(408).isRetryableFeedFailure())
        assertTrue(FeedHttpException(429).isRetryableFeedFailure())
        assertTrue(FeedHttpException(503).isRetryableFeedFailure())
    }

    @Test
    fun doesNotRetryPermanentHttpFailures() {
        assertFalse(FeedHttpException(400).isRetryableFeedFailure())
        assertFalse(FeedHttpException(404).isRetryableFeedFailure())
    }

    @Test
    fun retriesTransientNetworkFailures() {
        assertTrue(SocketTimeoutException().isRetryableFeedFailure())
        assertTrue(ConnectException().isRetryableFeedFailure())
        assertTrue(IOException().isRetryableFeedFailure())
    }

    @Test
    fun doesNotRetryParsingFailures() {
        assertFalse(IllegalStateException("invalid feed").isRetryableFeedFailure())
    }
}
