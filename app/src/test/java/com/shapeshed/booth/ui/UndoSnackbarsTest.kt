package com.shapeshed.booth.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * That a second swipe replaces the first snackbar rather than queueing behind it.
 *
 * The fake host here deliberately behaves like the real one: `SnackbarHostState` holds a fair
 * `Mutex`, so a second `showSnackbar` suspends until the first has been addressed and the first stays
 * actionable the whole time. That is the whole reason this class has to do something — the platform
 * queues, and without the cancellation in `UndoSnackbars.replace` it would keep queueing. Modelling
 * the fake as replacing instead would have made these tests pass against code that did nothing, which
 * is what the first version of this file did.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UndoSnackbarsTest {
    @Test
    fun takingTheShowingOfferRunsItsUndo() = withPresenter { snackbars, presenter ->
        var undone = 0
        snackbars.offer("removed") { undone++ }

        presenter.takeCurrent()

        assertEquals(1, undone)
    }

    @Test
    fun anOfferThatTimesOutRunsNoUndo() = withPresenter { snackbars, presenter ->
        var undone = false
        snackbars.offer("removed") { undone = true }

        presenter.expireCurrent()

        assertFalse(undone)
    }

    @Test
    fun aSecondOfferIsNotLeftWaitingBehindTheFirst() = withPresenter { snackbars, presenter ->
        // The discriminating assertion. Against a host that queues, a second offer would still be
        // blocked on the mutex and only the first would ever appear. Reaching "second" at all is
        // proof the first was cancelled rather than waited out.
        snackbars.offer("first") {}
        snackbars.offer("second") {}

        assertEquals(listOf("first", "second"), presenter.messages)
    }

    @Test
    fun aSecondOfferTakesOverSoOnlyTheNewestCanBeUndone() = withPresenter { snackbars, presenter ->
        var firstUndone = false
        var secondUndone = false

        snackbars.offer("first") { firstUndone = true }
        snackbars.offer("second") { secondUndone = true }
        presenter.takeCurrent()

        // The first swipe's undo is gone with its snackbar. That is the deliberate trade: a queue of
        // identical "Removed from Inbox / Undo" snackbars reads as broken, because pressing Undo looks
        // like it does nothing when an identical snackbar takes its place.
        assertTrue(secondUndone)
        assertFalse(firstUndone)
    }

    @Test
    fun aFailureReplacesTheShowingUndoRatherThanWaitingBehindIt() = withPresenter { snackbars, presenter ->
        var undone = false
        snackbars.offer("removed") { undone = true }

        snackbars.showMessage("could not remove")

        assertEquals(listOf("removed", "could not remove"), presenter.messages)
        presenter.takeCurrent()
        assertFalse(undone)
    }

    @Test
    fun aMessageWithNoUndoIsShownAndActingOnItRunsNothing() = withPresenter { snackbars, presenter ->
        snackbars.showMessage("it went wrong")

        assertEquals(listOf("it went wrong"), presenter.messages)
        presenter.takeCurrent()
    }

    /**
     * Runs [block] against a host that queues exactly as `SnackbarHostState` does: a fair mutex, so a
     * second presentation waits its turn and cancelling the caller releases it.
     */
    private fun withPresenter(block: (UndoSnackbars, QueueingPresenter) -> Unit) = runTest {
        val presenter = QueueingPresenter()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            block(UndoSnackbars(scope, presenter), presenter)
        } finally {
            scope.cancel()
        }
    }

    private class QueueingPresenter : UndoSnackbarPresenter {
        val messages = mutableListOf<String>()

        private val mutex = Mutex()
        private var current: CompletableDeferred<Boolean>? = null

        override suspend fun present(offer: UndoOffer): Boolean = mutex.withLock {
            val result = CompletableDeferred<Boolean>()
            current = result
            messages += offer.message
            result.await()
        }

        fun takeCurrent() {
            current?.complete(true)
        }

        fun expireCurrent() {
            current?.complete(false)
        }
    }
}
