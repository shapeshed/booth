package com.shapeshed.booth.ui

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.shapeshed.booth.R
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One pending "that happened, here is the way back" offer. */
internal class UndoOffer internal constructor(internal val message: String, internal val undo: suspend () -> Unit)

internal fun interface UndoSnackbarPresenter {
    /** Shows [offer] and resolves to whether its undo was taken rather than timed out. */
    suspend fun present(offer: UndoOffer): Boolean
}

/** Shows [offer] in [hostState] as a snackbar carrying [undoLabel] as its action. */
internal fun snackbarPresenter(
    hostState: SnackbarHostState,
    undoLabel: String,
    duration: SnackbarDuration = SnackbarDuration.Short,
): UndoSnackbarPresenter = UndoSnackbarPresenter { offer ->
    hostState.showSnackbar(
        message = offer.message,
        actionLabel = undoLabel,
        duration = duration,
    ) == SnackbarResult.ActionPerformed
}

/**
 * Shows one undo offer at a time, the newest replacing whatever was there, and runs the undos taken.
 *
 * A second swipe replaces the first offer, and that offer's undo goes with it. This is what Gmail
 * does, and it is wanted here: consecutive swipes all produce the same message, so a queue shows
 * several identical "Removed from Inbox / Undo" snackbars in sequence and pressing Undo appears to do
 * nothing, because the snackbar under the listener's thumb is swapped for another that looks identical.
 *
 * **The replacement has to happen here, because the platform does not do it.**
 * [SnackbarHostState.showSnackbar] queues rather than replaces: it holds a fair `Mutex`, so a second
 * call suspends until the first has been addressed and the first stays actionable throughout. An
 * earlier version of this class put its own queue in front of that, on the reasoning that a queued
 * offer is a fairer deal for the first listener. That was redundant and the stated reason was wrong:
 * the API already queues, so the earlier undo was never cancelled and never lost. Losing it requires
 * cancelling the caller, which is what [replace] does.
 *
 * Cancellation is what removes the visible snackbar. From [SnackbarHostState.showSnackbar]: "If the
 * caller is cancelled, the snackbar will be removed from display and/or the queue to be displayed."
 *
 * Cancelling is also why a replaced offer's [offer] undo never runs: the coroutine is abandoned
 * while suspended inside `showSnackbar`, so it never reaches the code that would call it.
 *
 * A message with no undo follows the same rule, so a failure replaces a pending undo rather than
 * waiting behind it for the earlier offer to time out. The cost is that a failure arriving inside
 * the undo window costs the listener that undo. The failures worth surfacing are rare, and a stale
 * "Removed from Inbox / Undo" for an action they no longer care about is the worse outcome.
 */
internal class UndoSnackbars(private val scope: CoroutineScope, private val presenter: UndoSnackbarPresenter) {
    private var showing: Job? = null
    private var pendingCommit: DeferredCommit? = null

    private class DeferredCommit(val commit: suspend () -> Unit)

    /**
     * Offers [undo] as the way back from something that already happened.
     *
     * [undo] is suspending because undoing usually means another write. It runs in this coroutine
     * rather than one of its own so a slow write cannot be overtaken by a later offer.
     */
    fun offer(message: String, undo: suspend () -> Unit) {
        replace {
            if (presenter.present(UndoOffer(message, undo))) undo()
        }
    }

    /**
     * Keeps a destructive action pending while its Undo snackbar is visible. The commit runs when
     * the snackbar expires or is replaced by a newer message; taking Undo runs [undo] instead.
     */
    fun offerDeferred(message: String, undo: suspend () -> Unit, commit: suspend () -> Unit) {
        val deferredCommit = DeferredCommit(commit)
        replace(deferredCommit) {
            val undoTaken = presenter.present(UndoOffer(message, undo))
            if (pendingCommit === deferredCommit) pendingCommit = null
            withContext(NonCancellable) {
                if (undoTaken) undo() else commit()
            }
        }
    }

    /** Shows a message with no way back, for a failure rather than an action. */
    fun showMessage(message: String) {
        replace { presenter.present(UndoOffer(message) {}) }
    }

    private fun replace(nextDeferredCommit: DeferredCommit? = null, block: suspend CoroutineScope.() -> Unit) {
        val replacedCommit = pendingCommit
        pendingCommit = nextDeferredCommit
        showing?.cancel()
        showing = scope.launch {
            replacedCommit?.let { deferred ->
                withContext(NonCancellable) { deferred.commit() }
            }
            block()
        }
    }
}

/**
 * The destructive actions a swipe can take, and the undo offered for each.
 *
 * Destructive swipes act immediately and offer undo. Downloads use a deferred commit so the bytes
 * remain available until the Undo snackbar expires.
 *
 * Takes the underlying operations as callbacks rather than the ViewModel. It was holding the
 * ViewModel only to forward to it, which is what the vm-forwarding rule flags, and it made this
 * class impossible to exercise without one.
 *
 * Framework-free by construction, so all of it is exercisable from a JVM unit test: no [Context], no
 * [SnackbarHostState], no Compose runtime. Strings arrive already resolved by [podcastUndoStrings].
 */
internal class PodcastHomeUndoActions(
    private val scope: CoroutineScope,
    private val snackbars: UndoSnackbars,
    private val strings: PodcastHomeUndoStrings,
    private val addToQueueFromInbox: (episodeId: Long, onAdded: () -> Unit, onError: () -> Unit) -> Unit,
    private val undoAddToQueueFromInbox: suspend (episodeId: Long) -> Result<Unit>,
    private val dismissFromInbox: (episodeId: Long) -> Unit,
    private val restoreToInbox: (episodeId: Long) -> Unit,
) {
    /**
     * Takes [apply] and, if it reports the action went through, offers to undo it.
     *
     * [apply] is awaited, and the offer is only made once it has run, so an undo is never offered
     * for something that did not occur: the queue removal reports false after rolling itself back,
     * and the listener is told the removal failed rather than being offered an undo that would undo
     * nothing. A caller that cannot fail returns true.
     */
    fun requestRemoval(message: String, apply: suspend () -> Boolean, undo: suspend () -> Unit) {
        scope.launch {
            if (apply()) snackbars.offer(message, undo)
        }
    }

    /** Adds an Inbox episode to Up next, offering a snackbar undo only after the add succeeds. */
    fun requestAddToQueue(episodeId: Long) {
        addToQueueFromInbox(
            episodeId,
            {
                snackbars.offer(strings.addedToNext) {
                    if (undoAddToQueueFromInbox(episodeId).isFailure) {
                        snackbars.showMessage(strings.removeFromNextFailed)
                    }
                }
            },
            { snackbars.showMessage(strings.addToNextFailed) },
        )
    }

    /**
     * Unfollows from the Subscriptions swipe: act now, leave a way back.
     *
     * Unfollowing is a soft delete, so [restore] is a flag flip on a row the database still holds
     * rather than a re-subscribe that would have to refetch the feed.
     */
    fun unfollowPodcastWithUndo(podcast: PodcastEntity, remove: () -> Unit, restore: () -> Unit) {
        requestRemoval(
            message = strings.unfollowed,
            apply = {
                remove()
                true
            },
            undo = restore,
        )
    }

    fun dismissInboxEpisode(episode: EpisodeEntity) {
        requestRemoval(
            message = strings.removedFromInbox,
            // Dismissing from the Inbox is a flag flip on a row the database still holds, so there is
            // no write that can fail here and nothing to roll back.
            apply = {
                dismissFromInbox(episode.id)
                true
            },
            undo = { restoreToInbox(episode.id) },
        )
    }

    fun removeDownloadWithUndo(undo: suspend () -> Unit, commit: suspend () -> Result<Unit>) {
        snackbars.offerDeferred(strings.downloadRemoved, undo) {
            if (commit().isFailure) snackbars.showMessage(strings.removeDownloadFailed)
        }
    }

    /**
     * Removes the current Inbox selection as one action, so one undo takes all of it back.
     *
     * Undo is per action rather than per row throughout: undoing one of five swipes out of a
     * selection would leave the listener with no way to tell which four they meant.
     */
    fun removeSelectedInboxEpisodes(inbox: List<EpisodeEntity>, selectedIds: Set<Long>, clearSelection: () -> Unit) {
        val selected = inbox.filter { it.id in selectedIds }
        if (selected.isEmpty()) {
            clearSelection()
            return
        }
        requestRemoval(
            message = strings.episodesRemovedFromInbox(selected.size),
            apply = {
                selected.forEach { dismissFromInbox(it.id) }
                clearSelection()
                true
            },
            undo = { selected.forEach { restoreToInbox(it.id) } },
        )
    }
}

/** The strings the undo actions show, which need the [Context] these classes deliberately lack. */
internal fun podcastUndoStrings(context: Context): PodcastHomeUndoStrings = PodcastHomeUndoStrings(
    undoLabel = context.getString(R.string.undo),
    removedFromInbox = context.getString(R.string.removed_from_inbox),
    episodesRemovedFromInbox = { count ->
        context.resources.getQuantityString(R.plurals.episodes_removed_from_inbox, count, count)
    },
    addToNextFailed = context.getString(R.string.error_add_up_next),
    addedToNext = context.getString(R.string.added_to_up_next),
    removeFromNextFailed = context.getString(R.string.error_remove_up_next),
    unfollowed = context.getString(R.string.unfollowed),
    downloadRemoved = context.getString(R.string.download_removed),
    removeDownloadFailed = context.getString(R.string.error_remove_download),
)

internal data class PodcastHomeUndoStrings(
    val undoLabel: String,
    val removedFromInbox: String,
    val episodesRemovedFromInbox: (count: Int) -> String,
    val addToNextFailed: String,
    val addedToNext: String,
    val removeFromNextFailed: String,
    val unfollowed: String,
    val downloadRemoved: String,
    val removeDownloadFailed: String,
)
