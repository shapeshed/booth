package com.shapeshed.booth.ui

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching], but preserves structured-concurrency cancellation.
 *
 * Cancellation is control flow, not an application failure. Keeping this rule in one small
 * helper makes ViewModel operations consistent and keeps the behavior straightforward to test.
 */
suspend inline fun <T> runCancellableCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }
