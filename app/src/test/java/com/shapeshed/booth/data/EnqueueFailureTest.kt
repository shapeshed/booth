package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How many times a failed download enqueue is retried.
 *
 * This pins the cap because the previous behaviour was an uncapped `Result.retry()`, which
 * reschedules indefinitely. Nothing in the test would have caught that except this file.
 */
class EnqueueFailureTest {
    @Test
    fun theFirstAttemptIsWorthRetrying() {
        assertEquals(EnqueueFailure.RETRY, classifyEnqueueFailure(attemptCount = 0))
    }

    @Test
    fun laterAttemptsAreStillRetriedWhileAttemptsRemain() {
        assertEquals(EnqueueFailure.RETRY, classifyEnqueueFailure(attemptCount = 1))
        assertEquals(EnqueueFailure.RETRY, classifyEnqueueFailure(attemptCount = 2))
    }

    /** The cap. Without this the worker would reschedule forever. */
    @Test
    fun theLastAttemptIsTheLastAttempt() {
        assertEquals(EnqueueFailure.GIVE_UP, classifyEnqueueFailure(attemptCount = 3))
    }

    @Test
    fun attemptsBeyondTheCapAreStillGivenUp() {
        assertEquals(EnqueueFailure.GIVE_UP, classifyEnqueueFailure(attemptCount = 4))
        assertEquals(EnqueueFailure.GIVE_UP, classifyEnqueueFailure(attemptCount = 99))
    }

    /** A nonsensical count must stop rather than loop. */
    @Test
    fun aNegativeAttemptCountGivesUp() {
        assertEquals(EnqueueFailure.GIVE_UP, classifyEnqueueFailure(attemptCount = -1))
    }

    @Test
    fun aCapOfZeroGivesUpImmediately() {
        assertEquals(EnqueueFailure.GIVE_UP, classifyEnqueueFailure(attemptCount = 0, maxAttempts = 0))
    }

    @Test
    fun theCapIsConfigurable() {
        assertEquals(EnqueueFailure.RETRY, classifyEnqueueFailure(attemptCount = 1, maxAttempts = 5))
        assertEquals(EnqueueFailure.GIVE_UP, classifyEnqueueFailure(attemptCount = 5, maxAttempts = 5))
    }

    @Test
    fun theDefaultCapIsThree() {
        assertEquals(3, MAX_ENQUEUE_ATTEMPTS)
    }
}
