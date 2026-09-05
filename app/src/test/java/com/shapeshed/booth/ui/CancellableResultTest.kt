package com.shapeshed.booth.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CancellableResultTest {
    @Test
    fun ordinaryFailureIsReturnedAsFailure() = runBlocking {
        val result = runCancellableCatching<Int> {
            error("network failure")
        }

        assertTrue(result.isFailure)
        assertEquals("network failure", result.exceptionOrNull()?.message)
    }

    @Test
    fun cancellationIsRethrown() = runBlocking {
        try {
            runCancellableCatching {
                throw CancellationException("request cancelled")
            }
            throw AssertionError("CancellationException was not rethrown")
        } catch (cancellation: CancellationException) {
            assertEquals("request cancelled", cancellation.message)
        }
    }
}
