package com.shapeshed.booth.ui

import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun PodcastHomeInboxSelectionActions(
    inbox: List<EpisodeEntity>,
    selectedIds: Set<Long>,
    menuExpanded: Boolean,
    viewModel: PodcastViewModel,
    undoActions: PodcastHomeUndoActions,
    podcastsById: Map<Long, PodcastEntity>,
    downloadProgress: Map<Long, DownloadProgress>,
    queueEpisodeIds: List<Long>,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onMenuExpandedChange: (Boolean) -> Unit,
    onClearSelection: () -> Unit,
    onPendingAction: (PodcastEpisodeAction) -> Unit,
    context: Context = LocalContext.current,
) {
    val addToUpNextError = stringResource(R.string.error_add_up_next)

    IconButton(onClick = {
        val selected = inbox.filter { it.id in selectedIds }
        selected.forEach {
            viewModel.addToQueueFromInbox(it.id, onError = {
                scope.launch {
                    snackbarHostState.showSnackbar(addToUpNextError)
                }
            })
        }
        onClearSelection()
    }) {
        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = stringResource(R.string.add_selected_to_up_next))
    }
    IconButton(onClick = {
        inbox.filter { it.id in selectedIds }.forEach { viewModel.download(context, it.id) }
        onClearSelection()
    }) {
        Icon(Icons.Rounded.FileDownload, contentDescription = stringResource(R.string.download_selected))
    }
    IconButton(onClick = {
        undoActions.removeSelectedInboxEpisodes(inbox, selectedIds, onClearSelection)
    }) {
        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.remove_selected_from_inbox))
    }
    Box {
        IconButton(onClick = { onMenuExpandedChange(true) }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_selection_actions))
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { onMenuExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.mark_played)) },
                trailingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                onClick = {
                    onMenuExpandedChange(false)
                    inbox.filter { it.id in selectedIds }.forEach { viewModel.markPlayed(it.id) }
                    onClearSelection()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.mark_unplayed)) },
                trailingIcon = { Icon(Icons.Rounded.Block, contentDescription = null) },
                onClick = {
                    onMenuExpandedChange(false)
                    inbox.filter { it.id in selectedIds }.forEach { viewModel.markUnplayed(it.id) }
                    onClearSelection()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reset_playback_position)) },
                trailingIcon = { Icon(Icons.Rounded.Replay10, contentDescription = null) },
                onClick = {
                    onMenuExpandedChange(false)
                    inbox.filter { it.id in selectedIds }.forEach { viewModel.markUnplayed(it.id) }
                    onClearSelection()
                },
            )
            if (selectedIds.size == 1) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.more_episode_actions)) },
                    trailingIcon = { Icon(Icons.Filled.MoreVert, contentDescription = null) },
                    onClick = {
                        onMenuExpandedChange(false)
                        inbox.firstOrNull { it.id in selectedIds }?.let { episode ->
                            onPendingAction(
                                createSubscribedEpisodeAction(
                                    episode = episode,
                                    podcastsById = podcastsById,
                                    downloadProgress = downloadProgress,
                                    queueEpisodeIds = queueEpisodeIds,
                                ),
                            )
                        }
                        onClearSelection()
                    },
                )
            }
        }
    }
}
