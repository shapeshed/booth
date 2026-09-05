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

@Composable
internal fun PodcastHomeEpisodeActionsOverlay(
    routeState: PodcastHomeRouteState,
    selectedTab: PodcastTab,
    context: Context,
    viewModel: PodcastViewModel,
    playbackViewModel: PodcastPlaybackViewModel,
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
                viewModel.resolveMediaSizes(action.episode) { sizes ->
                    downloadSizeBytes = sizes.first
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
            } else null,
            onShare = {
                action.linkUrl?.let { shareLink(context, action.episodeTitle, it) }
                routeState.pendingEpisodeAction.value = null
            },
            onOpenInBrowser = {
                action.linkUrl?.let { openExternally(context, it) }
                routeState.pendingEpisodeAction.value = null
            },
            onDownload = {
                when (action) {
                    is PodcastEpisodeAction.Subscribed -> viewModel.download(context, action.episode.id)
                    is PodcastEpisodeAction.Preview -> viewModel.downloadPreview(context, action.episode, action.podcast)
                }
                routeState.pendingEpisodeAction.value = null
            },
            onRemoveDownload = {
                when (action) {
                    is PodcastEpisodeAction.Subscribed -> viewModel.removeDownload(context, action.episode.id)
                    is PodcastEpisodeAction.Preview -> viewModel.preparePreviewEpisode(action.episode, action.podcast) { savedEpisode ->
                        viewModel.removeDownload(context, savedEpisode.id)
                    }
                }
                routeState.pendingEpisodeAction.value = null
            },
            onToggleQueue = {
                routeState.pendingEpisodeAction.value = null
                if (action.isInQueue) {
                    when (action) {
                        is PodcastEpisodeAction.Subscribed -> viewModel.removeFromQueue(action.episode.id)
                        is PodcastEpisodeAction.Preview -> viewModel.removeFromQueue(action.episode.id)
                    }
                    scope.launch { snackbarHostState.showSnackbar(context.getString(com.shapeshed.booth.R.string.removed_from_up_next), duration = SnackbarDuration.Short) }
                } else {
                    val showAddError = {
                        scope.launch {
                            snackbarHostState.showSnackbar(addToUpNextError)
                        }
                    }
                    when (action) {
                        is PodcastEpisodeAction.Subscribed -> viewModel.addToQueueFromInbox(action.episode.id, onError = { showAddError() })
                        is PodcastEpisodeAction.Preview -> viewModel.addPreviewToQueue(
                            episode = action.episode,
                            podcast = action.podcast,
                            onError = { showAddError() },
                        )
                    }
                }
            },
            onSetPlayed = { played ->
                routeState.pendingEpisodeAction.value = null
                when (action) {
                    is PodcastEpisodeAction.Subscribed -> if (played) {
                        viewModel.markPlayed(action.episode.id)
                    } else viewModel.markUnplayed(action.episode.id)
                    is PodcastEpisodeAction.Preview -> viewModel.preparePreviewEpisode(action.episode, action.podcast) { savedEpisode ->
                        if (played) viewModel.markPlayed(savedEpisode.id) else viewModel.markUnplayed(savedEpisode.id)
                    }
                }
            },
            onResetPosition = {
                routeState.pendingEpisodeAction.value = null
                when (action) {
                    is PodcastEpisodeAction.Subscribed -> {
                        viewModel.markUnplayed(action.episode.id)
                        playbackViewModel.resetPositionIfCurrent(action.episode.id)
                    }
                    is PodcastEpisodeAction.Preview -> viewModel.preparePreviewEpisode(action.episode, action.podcast) { savedEpisode ->
                        viewModel.markUnplayed(savedEpisode.id)
                        playbackViewModel.resetPositionIfCurrent(savedEpisode.id)
                    }
                }
            },
        )
    }
}
