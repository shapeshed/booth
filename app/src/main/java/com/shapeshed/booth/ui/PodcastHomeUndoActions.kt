package com.shapeshed.booth.ui

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The undoable inbox and subscription actions, and the snackbars that offer to undo them.
 *
 * Takes the four underlying operations as callbacks rather than the ViewModel. It was holding the
 * ViewModel only to forward to it, which is what the vm-forwarding rule flags, and it made this
 * class impossible to exercise without one.
 *
 * [addToQueueFromInbox] takes its failure callback as an argument, matching the ViewModel method it
 * stands in for, so that reporting the error stays the caller's decision rather than being baked in.
 */
internal class PodcastHomeUndoActions(
    private val scope: CoroutineScope,
    private val context: Context,
    private val snackbarHostState: SnackbarHostState,
    private val removePodcast: (PodcastEntity) -> Unit,
    private val addToQueueFromInbox: (episodeId: Long, onError: () -> Unit) -> Unit,
    private val dismissFromInbox: (episodeId: Long) -> Unit,
    private val restoreToInbox: (episodeId: Long) -> Unit,
) {
    fun requestPodcastRemoval(podcast: PodcastEntity) {
        removePodcast(podcast)
    }

    fun requestInboxAction(episode: EpisodeEntity, addToQueue: Boolean) {
        if (addToQueue) {
            addToQueueFromInbox(episode.id) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(com.shapeshed.booth.R.string.error_add_up_next),
                    )
                }
            }
            return
        }
        dismissFromInbox(episode.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.resources.getString(
                    com.shapeshed.booth.R.string.selected_episodes_removed_from_inbox,
                    1,
                    context.resources.getString(com.shapeshed.booth.R.string.episode_singular),
                ),
                actionLabel = context.getString(com.shapeshed.booth.R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) restoreToInbox(episode.id)
        }
    }

    fun removeSelectedInboxEpisodes(inbox: List<EpisodeEntity>, selectedIds: Set<Long>, clearSelection: () -> Unit) {
        val selected = inbox.filter { it.id in selectedIds }
        if (selected.isEmpty()) {
            clearSelection()
            return
        }
        selected.forEach { dismissFromInbox(it.id) }
        clearSelection()
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.resources.getString(
                    com.shapeshed.booth.R.string.selected_episodes_removed_from_inbox,
                    selected.size,
                    context.resources.getString(
                        if (selected.size ==
                            1
                        ) {
                            com.shapeshed.booth.R.string.episode_singular
                        } else {
                            com.shapeshed.booth.R.string.episode_plural
                        },
                    ),
                ),
                actionLabel = context.getString(com.shapeshed.booth.R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) selected.forEach { restoreToInbox(it.id) }
        }
    }
}
