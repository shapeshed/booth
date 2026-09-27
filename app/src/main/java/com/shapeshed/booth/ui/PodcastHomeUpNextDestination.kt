package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import kotlinx.coroutines.flow.StateFlow

/**
 * Adapts the up-next queue to its content.
 *
 * Takes the two queue actions as callbacks rather than the ViewModel. Two of them is small enough
 * that an action class would be ceremony, unlike the discovery screen, which needed ten.
 *
 * [onRemove] is suspend because removing from the queue has to survive the caller being cancelled
 * mid-drag; the ViewModel's implementation is where that guarantee lives.
 */
@Composable
internal fun PodcastHomeUpNextDestination(
    episodes: List<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    playback: PlaybackUiState,
    playbackProgress: PlaybackProgress,
    downloadProgress: Map<Long, DownloadProgress>,
    reorderMode: Boolean,
    filter: QueueFilter,
    onFilterChange: (QueueFilter) -> Unit,
    onOpen: (EpisodeEntity) -> Unit,
    onLongPress: (EpisodeEntity) -> Unit,
    onRemoveError: (EpisodeEntity) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onRemoveFromQueue: suspend (episodeId: Long) -> Result<Unit>,
    onDownload: (EpisodeEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    PodcastUpNextContent(
        episodes = episodes,
        podcastsById = podcastsById,
        activeEpisodeId = playback.episode?.id,
        activeProgress = playbackProgress.fraction,
        isPlaying = playback.isPlaying,
        isBuffering = playback.isBuffering,
        onOpen = onOpen,
        onRemove = onRemoveFromQueue,
        onRemoveError = onRemoveError,
        onReorder = onReorder,
        reorderMode = reorderMode,
        onLongPress = onLongPress,
        filter = filter,
        onFilterChange = onFilterChange,
        onPlay = onPlay,
        onDownload = onDownload,
        downloadProgress = downloadProgress,
        modifier = modifier,
    )
}

/**
 * Collects the played position for the up-next subtree only.
 *
 * The position ticks twice a second. Hoisting the collection into the home screen body would
 * recompose the whole screen at that rate, which is what splitting `PlaybackProgress` out of
 * `PlaybackUiState` was for. Same reasoning and same shape as
 * `ScopedPodcastHomeDiscoveryDestination`.
 */
@Composable
internal fun ScopedPodcastHomeUpNextDestination(
    episodes: List<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    playback: PlaybackUiState,
    playbackProgressFlow: StateFlow<PlaybackProgress>,
    downloadProgress: Map<Long, DownloadProgress>,
    reorderMode: Boolean,
    filter: QueueFilter,
    onFilterChange: (QueueFilter) -> Unit,
    onOpen: (EpisodeEntity) -> Unit,
    onLongPress: (EpisodeEntity) -> Unit,
    onRemoveError: (EpisodeEntity) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onRemoveFromQueue: suspend (episodeId: Long) -> Result<Unit>,
    onDownload: (EpisodeEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by playbackProgressFlow.collectAsStateWithLifecycle()
    PodcastHomeUpNextDestination(
        episodes = episodes,
        podcastsById = podcastsById,
        playback = playback,
        playbackProgress = progress,
        downloadProgress = downloadProgress,
        reorderMode = reorderMode,
        filter = filter,
        onFilterChange = onFilterChange,
        onOpen = onOpen,
        onLongPress = onLongPress,
        onRemoveError = onRemoveError,
        onReorder = onReorder,
        onPlay = onPlay,
        onRemoveFromQueue = onRemoveFromQueue,
        onDownload = onDownload,
        modifier = modifier,
    )
}
