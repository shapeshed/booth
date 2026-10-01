package com.shapeshed.booth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Reveals an item restored by Undo from its top edge, expanding and sliding down into place. */
@Composable
internal fun PodcastRestoreAnimation(
    restored: Boolean,
    modifier: Modifier = Modifier,
    startWhenReady: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Capture only the initial state: once the item starts entering, clearing the caller's restore
    // id must not reverse or cut short this transition.
    val visibilityState = remember { MutableTransitionState(!restored) }
    visibilityState.targetState = !restored || startWhenReady
    AnimatedVisibility(
        visibleState = visibilityState,
        modifier = modifier,
        enter = expandVertically(expandFrom = Alignment.Top) +
            slideInVertically(initialOffsetY = { -it }) +
            fadeIn(),
        exit = ExitTransition.None,
        content = { content() },
    )
}
