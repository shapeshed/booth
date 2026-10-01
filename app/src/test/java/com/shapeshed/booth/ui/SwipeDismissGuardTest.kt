package com.shapeshed.booth.ui

import androidx.compose.material3.SwipeToDismissBoxValue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rule that stops an undo from removing the thing it just restored.
 *
 * Regression test for a bug this found on a device rather than in a test: swiping an Inbox episode
 * away removed it, tapping Undo put it back, and then the returning row removed it again with no
 * gesture, so the undo looked like it had done nothing. Material3 saves `SwipeToDismissBoxState`, so
 * a row that leaves the list while it is still dismissed returns already dismissed and re-announces
 * itself. The row removes itself as part of the dismiss, so that path is the normal one, not an edge
 * case.
 */
class SwipeDismissGuardTest {
    @Test
    fun aFreshDismissIsActedOn() {
        assertEquals(
            SwipeDismissAction.Act,
            decideSwipeDismissAction(SwipeToDismissBoxValue.EndToStart, NO_DIRECTION_ACTED_ON),
        )
    }

    @Test
    fun bothDirectionsAreActedOnFromAFreshRow() {
        assertEquals(
            SwipeDismissAction.Act,
            decideSwipeDismissAction(SwipeToDismissBoxValue.StartToEnd, NO_DIRECTION_ACTED_ON),
        )
        assertEquals(
            SwipeDismissAction.Act,
            decideSwipeDismissAction(SwipeToDismissBoxValue.EndToStart, NO_DIRECTION_ACTED_ON),
        )
    }

    @Test
    fun aDismissAlreadyActedOnIsNotActedOnAgain() {
        // The regression. This is the returning row after an undo: the same direction, announced a
        // second time with no gesture. Acting on it removes the episode the undo just restored.
        val first = decideSwipeDismissAction(SwipeToDismissBoxValue.EndToStart, NO_DIRECTION_ACTED_ON)
        val actedOnOrdinal = SwipeToDismissBoxValue.EndToStart.ordinal

        assertEquals(SwipeDismissAction.Act, first)
        assertEquals(
            SwipeDismissAction.SettleAndRearm,
            decideSwipeDismissAction(SwipeToDismissBoxValue.EndToStart, actedOnOrdinal),
        )
    }

    @Test
    fun theOppositeDirectionIsStillItsOwnDismiss() {
        // Acting in one direction must not silence the other: the Inbox row swipes both ways.
        assertEquals(
            SwipeDismissAction.Act,
            decideSwipeDismissAction(
                SwipeToDismissBoxValue.StartToEnd,
                SwipeToDismissBoxValue.EndToStart.ordinal,
            ),
        )
    }

    @Test
    fun settlingIsIgnored() {
        assertEquals(
            SwipeDismissAction.Ignore,
            decideSwipeDismissAction(SwipeToDismissBoxValue.Settled, NO_DIRECTION_ACTED_ON),
        )
        // Even when it matches the acted-on direction, since "already acted on" must never turn a
        // settled row into something to re-arm.
        assertEquals(
            SwipeDismissAction.Ignore,
            decideSwipeDismissAction(SwipeToDismissBoxValue.Settled, SwipeToDismissBoxValue.Settled.ordinal),
        )
    }

    @Test
    fun aRowThatHasRearmedAcceptsTheSameDirectionAgain() {
        // The re-arm itself — clearing actedOnOrdinal back to the sentinel — happens in the row
        // composables, not in the function under test, so it is not pinned here. What this pins is
        // that the cleared value is accepted, which is the half that lives in the pure function.
        // The composable half is covered by the deliberate second swipe in PodcastSwipeRowTest.
        assertEquals(
            SwipeDismissAction.Act,
            decideSwipeDismissAction(SwipeToDismissBoxValue.EndToStart, NO_DIRECTION_ACTED_ON),
        )
    }
}
