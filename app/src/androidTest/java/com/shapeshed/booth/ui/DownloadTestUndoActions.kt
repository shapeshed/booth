package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope

/**
 * Undo plumbing for tests that render a screen with download rows.
 *
 * The downloads screen requires a [PodcastHomeUndoActions], and building one means a scope, a
 * presenter and the full string set. It was previously private to `PodcastSwipeRowTest`, so a second
 * test class wanting to render that screen had to duplicate all of it. Hoisted here so there is one
 * copy.
 */
@Composable
internal fun rememberTestDownloadUndoActions(
    scope: CoroutineScope,
    presenter: PendingUndoPresenter = remember { PendingUndoPresenter() },
): PodcastHomeUndoActions = remember(scope, presenter) {
    PodcastHomeUndoActions(
        scope = scope,
        snackbars = UndoSnackbars(scope, presenter),
        strings = PodcastHomeUndoStrings(
            undoLabel = "Undo",
            removedFromInbox = "Removed from Inbox",
            episodesRemovedFromInbox = { "$it episodes removed from Inbox" },
            addToNextFailed = "Could not add to Up next",
            addedToNext = "Added to Up next",
            removeFromNextFailed = "Could not remove from Up next",
            unfollowed = "Unfollowed",
            downloadRemoved = "Download removed",
            removeDownloadFailed = "Could not remove download",
        ),
        addToQueueFromInbox = { _, _, _ -> },
        undoAddToQueueFromInbox = { Result.success(Unit) },
        dismissFromInbox = {},
        restoreToInbox = {},
    )
}

/**
 * A presenter that parks until the test resolves it, so a removal can be observed while its undo
 * window is still open.
 */
internal class PendingUndoPresenter : UndoSnackbarPresenter {
    private var result: CompletableDeferred<Boolean>? = null

    override suspend fun present(offer: UndoOffer): Boolean =
        CompletableDeferred<Boolean>().also { result = it }.await()

    fun resolve(undo: Boolean) {
        result?.complete(undo)
    }
}
