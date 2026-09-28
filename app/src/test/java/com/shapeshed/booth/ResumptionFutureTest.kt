package com.shapeshed.booth

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * [resumptionFuture] exists because the work behind it resolves a Media3 playback-resumption
 * request, and the client waiting on it is outside the app: Android Auto, the assistant, a
 * Bluetooth device. A future left pending hangs that client with no error and no timeout to save
 * it.
 *
 * These are JVM tests, not instrumentation, because the behaviour under test is the future's
 * lifetime rather than anything Android does. The whole class is cheap and deterministic, which is
 * the point of having extracted it.
 */
class ResumptionFutureTest {
    private fun liveScope() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Test
    fun completesWithTheComputedValue() {
        val future = resumptionFuture(liveScope()) { "resumed" }

        assertEquals("resumed", future.get(1, TimeUnit.SECONDS))
    }

    @Test
    fun completesExceptionallyWhenTheWorkThrows() {
        val boom = IllegalStateException("no such episode")

        val future = resumptionFuture(liveScope()) { throw boom }

        assertEquals(boom, thrownBy(future).cause)
    }

    /**
     * The regression this helper was extracted for.
     *
     * A coroutine launched into an already-cancelled scope never runs its body, so nothing would
     * resolve the completer and the future would sit pending forever. `CallbackToFutureAdapter` has
     * no `cancel()`, so there is nothing to time it out. Before the fix this was a `TimeoutException`
     * here, and a permanently hung car in the real world.
     */
    @Test
    fun completesEvenWhenTheScopeWasAlreadyCancelled() {
        val scope = liveScope()
        scope.cancel()

        val future = resumptionFuture(scope) { "never runs" }

        thrownBy(future)
    }

    /** The same window reached the other way round: cancelled while the work is in flight. */
    @Test
    fun completesWhenTheScopeIsCancelledWhileTheWorkIsInFlight() {
        val scope = liveScope()
        val started = CompletableDeferred<Unit>()
        val future = resumptionFuture(scope) {
            started.complete(Unit)
            delay(Long.MAX_VALUE)
            "never returns"
        }
        runBlocking { started.await() }

        scope.cancel()

        thrownBy(future, seconds = 2)
    }

    /**
     * A service shutdown is not a resumption failure the client should be told about, and
     * `runCatching` used to report it as one. Cancellation now surfaces as cancellation, so the
     * cause is the CancellationException itself rather than an unrelated error.
     */
    @Test
    fun reportsCancellationAsCancellationRatherThanAsAFailure() {
        val future = resumptionFuture(liveScope()) { throw CancellationException("scope went away") }

        val cause = thrownBy(future).cause
        assertTrue("expected a CancellationException but was $cause", cause is CancellationException)
    }

    /** If the client walks away, stop doing database work on its behalf. */
    @Test
    fun cancellingTheFutureCancelsTheWork() {
        val scope = liveScope()
        val started = CompletableDeferred<Unit>()
        val observedCancellation = CompletableDeferred<Unit>()
        val future = resumptionFuture(scope) {
            started.complete(Unit)
            try {
                delay(Long.MAX_VALUE)
            } catch (cancelled: CancellationException) {
                observedCancellation.complete(Unit)
                throw cancelled
            }
            "never returns"
        }
        runBlocking { started.await() }

        future.cancel(true)

        // Fails the test if the work was not cancelled, which is the behaviour being pinned.
        runBlocking { withTimeout(2_000) { observedCancellation.await() } }
    }

    /**
     * Asserts the future completes, and returns the [ExecutionException] it completed with.
     *
     * A timeout is reported as the failure it is rather than as a generic error, because hanging is
     * the specific bug these tests exist to catch.
     */
    private fun thrownBy(future: ListenableFuture<*>, seconds: Long = 1): ExecutionException {
        try {
            future.get(seconds, TimeUnit.SECONDS)
        } catch (expected: ExecutionException) {
            return expected
        } catch (hung: TimeoutException) {
            fail("the future never completed within ${seconds}s - this is the bug these tests prevent")
        }
        fail("expected the future to complete exceptionally, but it returned normally")
        error("unreachable: fail() always throws")
    }
}
