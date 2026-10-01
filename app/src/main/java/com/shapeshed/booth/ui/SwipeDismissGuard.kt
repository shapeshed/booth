package com.shapeshed.booth.ui

import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember

/** Sentinel for "this row has not acted on a dismiss in either direction yet". */
internal const val NO_DIRECTION_ACTED_ON = -1

/**
 * What a swipe row should do about a dismiss that has just been announced to it.
 */
internal sealed interface SwipeDismissAction {
    /** A dismiss the listener just made. Do the thing, then settle the row. */
    data object Act : SwipeDismissAction

    /**
     * A dismiss this row has already acted on, announced a second time with no gesture.
     *
     * Settle the row back and re-arm, but do not act again.
     */
    data object SettleAndRearm : SwipeDismissAction

    /** Nothing to act on. */
    data object Ignore : SwipeDismissAction
}

/**
 * Decides what a swipe row does with a dismiss announcement.
 *
 * Material3 keeps `SwipeToDismissBoxState.currentValue` in saveable state, so a row that leaves the
 * list while it is still dismissed comes back already set to [SwipeToDismissBoxValue.EndToStart], and
 * `SwipeToDismissBox` announces that value to `onDismiss` again with no gesture behind it. Every
 * destructive row here removes its own item as part of the dismiss, which is exactly that case. The
 * resulting loop is: swipe, undo, and the returning row removes the episode the undo just restored, so
 * the undo looks like it did nothing. That is the bug this decides against, and it is why the
 * comparison is against the direction already acted on rather than a simple "ignore if not settled".
 *
 * Compared by ordinal so the sentinel can be an Int and the pair can live in ordinary mutable
 * state rather than needing a `Bundle` type.
 *
 * With [rememberRowSwipeDismissState] there should be nothing to re-announce, because a dismissed
 * state no longer survives the row's removal. This is the belt to that pair of braces.
 */
internal fun decideSwipeDismissAction(direction: SwipeToDismissBoxValue, actedOnOrdinal: Int): SwipeDismissAction =
    when {
        direction == SwipeToDismissBoxValue.Settled -> SwipeDismissAction.Ignore
        direction.ordinal == actedOnOrdinal -> SwipeDismissAction.SettleAndRearm
        else -> SwipeDismissAction.Act
    }

/**
 * The direction this row has already acted on.
 *
 * Ordinary composition state, deliberately, and it has to match the lifetime of the dismiss state it
 * guards — which is what [rememberRowSwipeDismissState] also makes ordinary. Persisting it with
 * [rememberSaveable] while the dismiss state is not persisted buys nothing, because the only thing it
 * could outlive is a row whose state was thrown away. It then outlives the row and keeps refusing the
 * listener's next swipe in that direction: the row comes back from an undo, the guard still remembers
 * the dismissal from before, and the swipe that should remove it is silently swallowed.
 */
@Composable
internal fun rememberSwipeDismissGuard(): MutableIntState = remember { mutableIntStateOf(NO_DIRECTION_ACTED_ON) }

/**
 * A dismiss state for a row that is allowed to forget it once the row leaves the list.
 *
 * [rememberSwipeToDismissBoxState] keeps `currentValue` in saveable state, and a lazy list preserves
 * that state under the item's key. Every destructive row here removes its own item as part of the
 * dismiss, so the row is disposed while it is still set to
 * [SwipeToDismissBoxValue.EndToStart] — and an undo puts the same episode straight back, into the same
 * key, which hands the row its own dismissed state again. `SwipeToDismissBox` then announces that to
 * `onDismiss` with no gesture behind it, and until the row is settled again it renders translated off
 * screen: present in the list, correct in the database, invisible to the listener and unswipeable. That
 * reads exactly like an undo that did nothing, which is how it was reported.
 *
 * Building the state with plain [remember] makes it ordinary composition state instead, so it is
 * discarded with the row and a returning row always starts settled. There is nothing to resurrect,
 * so there is nothing to recover from, and [rememberSwipeDismissGuard] is left as defence against a
 * re-announcement that happens while the row is still composed.
 *
 * The cost is that a row scrolled out and back mid-swipe snaps back to centre instead of resuming its
 * slide. For a list of episodes that is the better failure anyway: a row that comes back looking
 * half-swiped is ambiguous, and the listener who meant to swipe can simply do it again.
 */
@Composable
internal fun rememberRowSwipeDismissState(): SwipeToDismissBoxState = remember {
    SwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = { distance -> distance * SWIPE_TO_DISMISS_THRESHOLD_FRACTION },
    )
}
