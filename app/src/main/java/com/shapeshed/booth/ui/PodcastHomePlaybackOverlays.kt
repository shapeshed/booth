package com.shapeshed.booth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PodcastHomeMiniPlayerOverlay(
    visible: Boolean,
    episode: com.shapeshed.booth.data.EpisodeEntity?,
    podcastTitle: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    playbackViewModel: PodcastPlaybackViewModel,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
                .onSizeChanged { onHeightChanged(it.height) }
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        ) {
            episode?.let {
                val haptic = LocalHapticFeedback.current
                val dismissState = rememberSwipeToDismissBoxState()
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                        playbackViewModel.clearRememberedEpisode()
                        playbackViewModel.stopAndClear()
                    }
                }
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = FloatingToolbarDefaults.ContainerShape,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Box(
                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                        Alignment.CenterStart
                                    } else Alignment.CenterEnd,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription = stringResource(com.shapeshed.booth.R.string.stop_playback),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    },
                ) {
                    PodcastMiniPlayer(
                        episode = it,
                        podcastTitle = podcastTitle,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        onPlayPause = playbackViewModel::togglePlayPause,
                        onOpen = onOpen,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PodcastHomeNowPlayingOverlay(
    visible: Boolean,
    playback: PlaybackUiState,
    sleepTimer: com.shapeshed.booth.data.SleepTimerState?,
    podcastTitle: String?,
    onOpenPodcast: (Long) -> Unit,
    playbackViewModel: PodcastPlaybackViewModel,
    viewModel: PodcastViewModel,
    queueEpisodes: List<com.shapeshed.booth.data.EpisodeEntity>,
    podcastsById: Map<Long, com.shapeshed.booth.data.PodcastEntity>,
    podcastTitlesById: Map<Long, String>,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible && playback.episode != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.fillMaxSize(),
    ) {
        playback.episode?.let { episode ->
            PodcastNowPlayingOverlay(
                episode = episode,
                podcastTitle = podcastTitle,
                podcastArtworkUrl = podcastsById[episode.podcastId]?.artworkUrl,
                onOpenPodcast = { onOpenPodcast(episode.podcastId) },
                isPlaying = playback.isPlaying,
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
                player = playbackViewModel.player,
                videoMode = playback.isVideoMode,
                isBuffering = playback.isBuffering,
                playbackError = playback.playbackError,
                onToggle = playbackViewModel::togglePlayPause,
                onVideoModeChange = playbackViewModel::setVideoMode,
                speed = playback.speed,
                skipSilence = playback.skipSilence,
                sleepTimer = sleepTimer,
                onSetSleepTimer = playbackViewModel::setSleepTimer,
                onCancelSleepTimer = playbackViewModel::cancelSleepTimer,
                onSpeedChange = playbackViewModel::setSpeed,
                onSkipSilenceChange = playbackViewModel::setSkipSilence,
                onSeekBack = playbackViewModel::seekBack,
                onSeekForward = playbackViewModel::seekForward,
                onSeekTo = playbackViewModel::seekTo,
                onRetry = playbackViewModel::retryPlayback,
                onDismiss = onDismiss,
                queueEpisodes = queueEpisodes,
                queuePodcasts = podcastsById,
                queueActiveProgress = playback.durationMs.takeIf { it > 0L }?.let {
                    (playback.positionMs.toFloat() / it).coerceIn(0f, 1f)
                },
                onQueuePlay = { queued ->
                    playbackViewModel.playQueue(
                        episodes = queueEpisodes,
                        selectedEpisode = queued,
                        podcastTitles = podcastTitlesById,
                        useVideo = (queued.preferVideo || (!queued.videoPreferenceSet && queued.isVideoOnlySource())) &&
                            !queued.videoUrl.isNullOrBlank(),
                    )
                },
                onQueueRemove = viewModel::removeFromQueue,
                onQueueReorder = viewModel::reorderQueue,
    )
}
    }
}
