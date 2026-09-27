package com.shapeshed.booth.ui

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * What can be done to the episode in the action sheet.
 *
 * The sheet's whole job is acting on an episode, and every action has to answer the same question
 * first: is this a subscribed episode, which already has a saved row, or a preview of one that
 * does not? A preview has to be saved before most things can be done to it, and each of the five
 * actions below was spelling that out again in its own `when`. The branching lives here instead,
 * once, which is the point: the sheet is now a list of callbacks with no knowledge of the
 * difference between the two kinds of episode.
 */
internal class PodcastEpisodeActionActions(
    val resolveMediaSizes: (action: PodcastEpisodeAction, onSizes: (audioBytes: Long?) -> Unit) -> Unit,
    val download: (action: PodcastEpisodeAction) -> Unit,
    val removeDownload: (action: PodcastEpisodeAction) -> Unit,
    val removeFromQueue: (action: PodcastEpisodeAction) -> Unit,
    val addToQueue: (action: PodcastEpisodeAction, onError: () -> Unit) -> Unit,
    val setPlayed: (action: PodcastEpisodeAction, played: Boolean) -> Unit,
    val resetPosition: (action: PodcastEpisodeAction) -> Unit,
)

/** Builds the action-sheet actions where the ViewModels are in scope. */
@Composable
internal fun rememberEpisodeActionActions(
    context: Context,
    viewModel: PodcastViewModel,
    playbackViewModel: PodcastPlaybackViewModel,
): PodcastEpisodeActionActions = remember(context, viewModel, playbackViewModel) {
    PodcastEpisodeActionActions(
        resolveMediaSizes = { action, onSizes ->
            when (action) {
                is PodcastEpisodeAction.Subscribed -> viewModel.resolveMediaSizes(action.episode) { sizes ->
                    onSizes(sizes.first)
                }

                // A preview has no saved row to measure yet, so the caller leaves the size unknown.
                is PodcastEpisodeAction.Preview -> onSizes(null)
            }
        },
        download = { action ->
            when (action) {
                is PodcastEpisodeAction.Subscribed -> viewModel.download(context, action.episode.id)

                is PodcastEpisodeAction.Preview -> viewModel.downloadPreview(
                    context,
                    action.episode,
                    action.podcast,
                )
            }
        },
        removeDownload = { action ->
            when (action) {
                is PodcastEpisodeAction.Subscribed -> viewModel.removeDownload(context, action.episode.id)

                is PodcastEpisodeAction.Preview -> viewModel.preparePreviewEpisode(
                    action.episode,
                    action.podcast,
                ) { savedEpisode -> viewModel.removeDownload(context, savedEpisode.id) }
            }
        },
        removeFromQueue = { action ->
            // Both branches do the same thing, because by this point a preview has a saved row too.
            // The when is only here because the episode lives on the two subtypes.
            when (action) {
                is PodcastEpisodeAction.Subscribed -> viewModel.removeFromQueue(action.episode.id)
                is PodcastEpisodeAction.Preview -> viewModel.removeFromQueue(action.episode.id)
            }
        },
        addToQueue = { action, onError ->
            when (action) {
                is PodcastEpisodeAction.Subscribed -> viewModel.addToQueueFromInbox(
                    episodeId = action.episode.id,
                    onError = onError,
                )

                is PodcastEpisodeAction.Preview -> viewModel.addPreviewToQueue(
                    episode = action.episode,
                    podcast = action.podcast,
                    onError = onError,
                )
            }
        },
        setPlayed = { action, played ->
            when (action) {
                is PodcastEpisodeAction.Subscribed -> if (played) {
                    viewModel.markPlayed(action.episode.id)
                } else {
                    viewModel.markUnplayed(action.episode.id)
                }

                is PodcastEpisodeAction.Preview -> viewModel.preparePreviewEpisode(
                    action.episode,
                    action.podcast,
                ) { savedEpisode ->
                    if (played) viewModel.markPlayed(savedEpisode.id) else viewModel.markUnplayed(savedEpisode.id)
                }
            }
        },
        resetPosition = { action ->
            when (action) {
                is PodcastEpisodeAction.Subscribed -> {
                    viewModel.markUnplayed(action.episode.id)
                    playbackViewModel.resetPositionIfCurrent(action.episode.id)
                }

                is PodcastEpisodeAction.Preview -> viewModel.preparePreviewEpisode(
                    action.episode,
                    action.podcast,
                ) { savedEpisode ->
                    viewModel.markUnplayed(savedEpisode.id)
                    playbackViewModel.resetPositionIfCurrent(savedEpisode.id)
                }
            }
        },
    )
}

@Composable
internal fun PodcastHomeEpisodeActionsOverlay(
    routeState: PodcastHomeRouteState,
    selectedTab: PodcastTab,
    context: Context,
    actions: PodcastEpisodeActionActions,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
) {
    val addToUpNextError = stringResource(com.shapeshed.booth.R.string.error_add_up_next)
    routeState.pendingEpisodeAction.value?.let { action ->
        var downloadSizeBytes by remember(action) { mutableStateOf(action.downloadSizeBytes) }
        var loadingDownloadSize by remember(action) { mutableStateOf(false) }
        LaunchedEffect(action) {
            if (action is PodcastEpisodeAction.Subscribed &&
                action.downloadSizeBytes == null
            ) {
                loadingDownloadSize = true
                actions.resolveMediaSizes(action) { size ->
                    downloadSizeBytes = size
                    loadingDownloadSize = false
                }
            }
        }
        PodcastEpisodeActionsSheet(
            action = action,
            downloadSizeBytes = downloadSizeBytes,
            loadingDownloadSize = loadingDownloadSize,
            onDismiss = { routeState.pendingEpisodeAction.value = null },
            onReorder = if (action.isInQueue && selectedTab == PodcastTab.UP_NEXT) {
                {
                    routeState.pendingEpisodeAction.value = null
                    routeState.queueReorderMode.value = true
                }
            } else {
                null
            },
            onShare = {
                action.linkUrl?.let { shareLink(context, action.episodeTitle, it) }
                routeState.pendingEpisodeAction.value = null
            },
            onOpenInBrowser = {
                action.linkUrl?.let { openExternally(context, it) }
                routeState.pendingEpisodeAction.value = null
            },
            onDownload = {
                actions.download(action)
                routeState.pendingEpisodeAction.value = null
            },
            onRemoveDownload = {
                actions.removeDownload(action)
                routeState.pendingEpisodeAction.value = null
            },
            onToggleQueue = {
                routeState.pendingEpisodeAction.value = null
                if (action.isInQueue) {
                    actions.removeFromQueue(action)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(com.shapeshed.booth.R.string.removed_from_up_next),
                            duration = SnackbarDuration.Short,
                        )
                    }
                } else {
                    actions.addToQueue(action) {
                        scope.launch {
                            snackbarHostState.showSnackbar(addToUpNextError)
                        }
                    }
                }
            },
            onSetPlayed = { played ->
                routeState.pendingEpisodeAction.value = null
                actions.setPlayed(action, played)
            },
            onResetPosition = {
                routeState.pendingEpisodeAction.value = null
                actions.resetPosition(action)
            },
        )
    }
}
