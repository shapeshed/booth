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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PodcastHomeMiniPlayerOverlay(
    visible: Boolean,
    episode: com.shapeshed.booth.data.EpisodeEntity?,
    podcastTitle: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTogglePlayPause: () -> Unit,
    onClearRememberedEpisode: () -> Unit,
    onStopAndClear: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onHeightChange: (Int) -> Unit,
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
                .onSizeChanged { onHeightChange(it.height) }
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        ) {
            episode?.let {
                val haptic = LocalHapticFeedback.current
                val dismissState = rememberSwipeToDismissBoxState()
                // The dismiss effect is keyed only on the swipe state, so it does not restart when
                // these change. Without remembering them it would run with whichever instances were
                // captured the last time the key changed, which is a stale callback rather than the
                // current one.
                val currentOnDismiss by rememberUpdatedState(onDismiss)
                val currentOnClearRememberedEpisode by rememberUpdatedState(onClearRememberedEpisode)
                val currentOnStopAndClear by rememberUpdatedState(onStopAndClear)
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnDismiss()
                        currentOnClearRememberedEpisode()
                        currentOnStopAndClear()
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
                                    contentAlignment = if (dismissState.dismissDirection ==
                                        SwipeToDismissBoxValue.StartToEnd
                                    ) {
                                        Alignment.CenterStart
                                    } else {
                                        Alignment.CenterEnd
                                    },
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
                        onPlayPause = onTogglePlayPause,
                        onOpen = onOpen,
                    )
                }
            }
        }
    }
}

/**
 * What the now-playing sheet can do.
 *
 * The sheet is the app's densest control surface: transport, seek, video mode, speed, skip silence,
 * the sleep timer, and the queue underneath it. It was taking both ViewModels to reach all of that,
 * which is the forwarding the rule flags and which made the sheet untestable without Hilt.
 *
 * [playFromQueue] takes the queue and the titles rather than closing over them, so this class holds
 * no caller state and can be remembered against the ViewModels alone. The video decision is worked
 * out inside it, because deciding whether a queued episode should start in video mode is a rule
 * about the episode, not about the sheet.
 */
internal class PodcastNowPlayingActions(
    val togglePlayPause: () -> Unit,
    val retryPlayback: () -> Unit,
    val setVideoMode: (Boolean) -> Unit,
    val setSkipSilence: (Boolean) -> Unit,
    val setSleepTimer: (durationMs: Long) -> Unit,
    val cancelSleepTimer: () -> Unit,
    val seekBack: () -> Unit,
    val seekForward: () -> Unit,
    val seekTo: (positionMs: Long) -> Unit,
    val setSpeed: (podcastId: Long, speed: Float?) -> Unit,
    val playFromQueue: (
        queued: com.shapeshed.booth.data.EpisodeEntity,
        episodes: List<com.shapeshed.booth.data.EpisodeEntity>,
        podcastTitles: Map<Long, String>,
    ) -> Unit,
    val removeFromQueue: (episodeId: Long) -> Unit,
    val reorderQueue: (episodeIds: List<Long>) -> Unit,
)

/** Builds the now-playing actions where the ViewModels are in scope. */
@Composable
internal fun rememberNowPlayingActions(
    playbackViewModel: PodcastPlaybackViewModel,
    viewModel: PodcastViewModel,
): PodcastNowPlayingActions = remember(playbackViewModel, viewModel) {
    PodcastNowPlayingActions(
        togglePlayPause = playbackViewModel::togglePlayPause,
        retryPlayback = playbackViewModel::retryPlayback,
        setVideoMode = playbackViewModel::setVideoMode,
        setSkipSilence = playbackViewModel::setSkipSilence,
        setSleepTimer = playbackViewModel::setSleepTimer,
        cancelSleepTimer = playbackViewModel::cancelSleepTimer,
        seekBack = playbackViewModel::seekBack,
        seekForward = playbackViewModel::seekForward,
        seekTo = playbackViewModel::seekTo,
        setSpeed = playbackViewModel::setPodcastPlaybackSpeed,
        playFromQueue = { queued, episodes, podcastTitles ->
            playbackViewModel.playQueue(
                episodes = episodes,
                selectedEpisode = queued,
                podcastTitles = podcastTitles,
                useVideo = (queued.preferVideo || (!queued.videoPreferenceSet && queued.isVideoOnlySource())) &&
                    !queued.videoUrl.isNullOrBlank(),
            )
        },
        removeFromQueue = viewModel::removeFromQueue,
        reorderQueue = viewModel::reorderQueue,
    )
}

@Composable
internal fun PodcastHomeNowPlayingOverlay(
    visible: Boolean,
    playback: PlaybackUiState,
    sleepTimer: com.shapeshed.booth.data.SleepTimerState?,
    podcastTitle: String?,
    onOpenPodcast: (Long) -> Unit,
    actions: PodcastNowPlayingActions,
    player: androidx.media3.common.Player?,
    playbackProgressFlow: kotlinx.coroutines.flow.StateFlow<PlaybackProgress>,
    queueEpisodes: List<com.shapeshed.booth.data.EpisodeEntity>,
    podcastsById: Map<Long, com.shapeshed.booth.data.PodcastEntity>,
    podcastTitlesById: Map<Long, String>,
    onDismiss: () -> Unit,
) {
    // Collected here rather than in a parent: this is the only overlay that draws a scrubber, and
    // routing the 2 Hz position through a parent would recompose that parent's whole subtree.
    val progress by playbackProgressFlow.collectAsStateWithLifecycle()
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
                onOpenPodcast = { onOpenPodcast(episode.podcastId) },
                isPlaying = playback.isPlaying,
                positionMs = progress.positionMs,
                durationMs = progress.durationMs,
                player = player,
                videoMode = playback.isVideoMode,
                isBuffering = playback.isBuffering,
                playbackError = playback.playbackError,
                onToggle = actions.togglePlayPause,
                onVideoModeChange = actions.setVideoMode,
                speed = playback.speed,
                skipSilence = playback.skipSilence,
                sleepTimer = sleepTimer,
                onSetSleepTimer = actions.setSleepTimer,
                onCancelSleepTimer = actions.cancelSleepTimer,
                onSpeedChange = { speed -> actions.setSpeed(episode.podcastId, speed) },
                onSkipSilenceChange = actions.setSkipSilence,
                onSeekBack = actions.seekBack,
                onSeekForward = actions.seekForward,
                onSeekTo = actions.seekTo,
                onRetry = actions.retryPlayback,
                onDismiss = onDismiss,
                queueEpisodes = queueEpisodes,
                queuePodcasts = podcastsById,
                queueActiveProgress = playback.episode?.let { progress.fraction },
                onQueuePlay = { queued -> actions.playFromQueue(queued, queueEpisodes, podcastTitlesById) },
                onQueueRemove = actions.removeFromQueue,
                onQueueReorder = actions.reorderQueue,
            )
        }
    }
}
