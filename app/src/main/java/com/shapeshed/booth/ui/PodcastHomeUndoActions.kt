package com.shapeshed.booth.ui

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class PodcastHomeUndoActions(
    private val scope: CoroutineScope,
    private val context: Context,
    private val snackbarHostState: SnackbarHostState,
    private val viewModel: PodcastViewModel,
) {
    fun requestPodcastRemoval(podcast: PodcastEntity) {
        viewModel.remove(podcast)
    }

    fun requestInboxAction(episode: EpisodeEntity, addToQueue: Boolean) {
        if (addToQueue) {
            viewModel.addToQueueFromInbox(
                episode.id,
                onError = {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(com.shapeshed.booth.R.string.error_add_up_next))
                    }
                },
            )
            return
        }
        viewModel.dismissFromInbox(episode.id)
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
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreToInbox(episode.id)
        }
    }

    fun removeSelectedInboxEpisodes(
        inbox: List<EpisodeEntity>,
        selectedIds: Set<Long>,
        clearSelection: () -> Unit,
    ) {
        val selected = inbox.filter { it.id in selectedIds }
        if (selected.isEmpty()) {
            clearSelection()
            return
        }
        selected.forEach { viewModel.dismissFromInbox(it.id) }
        clearSelection()
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.resources.getString(
                    com.shapeshed.booth.R.string.selected_episodes_removed_from_inbox,
                    selected.size,
                    context.resources.getString(if (selected.size == 1) com.shapeshed.booth.R.string.episode_singular else com.shapeshed.booth.R.string.episode_plural),
                ),
                actionLabel = context.getString(com.shapeshed.booth.R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) selected.forEach { viewModel.restoreToInbox(it.id) }
        }
    }
}
