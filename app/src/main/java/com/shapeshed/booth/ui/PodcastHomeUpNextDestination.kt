package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.booth.data.DownloadAssetEntity
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
 * Remove and restore are both suspend because each has to survive the caller being cancelled; the
 * ViewModel's implementations are where that guarantee lives. The undo is offered only once the
 * removal has been awaited and reported success, so a listener is never handed an undo for a removal
 * that rolled itself back.
 */
@Composable
internal fun PodcastHomeUpNextDestination(
    episodes: List<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    playback: PlaybackUiState,
    playbackProgress: PlaybackProgress,
    downloadProgress: Map<Long, DownloadProgress>,
    downloadAssets: Map<Long, DownloadAssetEntity> = emptyMap(),
    reorderMode: Boolean,
    filter: QueueFilter,
    onFilterChange: (QueueFilter) -> Unit,
    onOpen: (EpisodeEntity) -> Unit,
    onLongPress: (EpisodeEntity) -> Unit,
    onRemoveError: (EpisodeEntity) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onRemoveFromQueue: suspend (episodeId: Long) -> Result<Unit>,
    onRestoreToQueue: suspend (episodeId: Long, position: Int) -> Result<Unit>,
    onDownload: (EpisodeEntity) -> Unit,
    onRetryDownload: (episodeId: Long) -> Unit = {},
    onRemoveDownload: suspend (episodeId: Long) -> Result<Unit> = { Result.success(Unit) },
    undoActions: PodcastHomeUndoActions,
    removedFromUpNextMessage: String,
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
        onRestoreToQueue = onRestoreToQueue,
        onRemoveError = onRemoveError,
        onReorder = onReorder,
        reorderMode = reorderMode,
        onLongPress = onLongPress,
        filter = filter,
        onFilterChange = onFilterChange,
        onPlay = onPlay,
        onDownload = onDownload,
        downloadProgress = downloadProgress,
        downloadAssets = downloadAssets,
        onRetryDownload = onRetryDownload,
        onRemoveDownload = onRemoveDownload,
        undoActions = undoActions,
        removedFromUpNextMessage = removedFromUpNextMessage,
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
    downloadAssets: Map<Long, DownloadAssetEntity> = emptyMap(),
    reorderMode: Boolean,
    filter: QueueFilter,
    onFilterChange: (QueueFilter) -> Unit,
    onOpen: (EpisodeEntity) -> Unit,
    onLongPress: (EpisodeEntity) -> Unit,
    onRemoveError: (EpisodeEntity) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onRemoveFromQueue: suspend (episodeId: Long) -> Result<Unit>,
    onRestoreToQueue: suspend (episodeId: Long, position: Int) -> Result<Unit>,
    onDownload: (EpisodeEntity) -> Unit,
    onRetryDownload: (episodeId: Long) -> Unit = {},
    onRemoveDownload: suspend (episodeId: Long) -> Result<Unit> = { Result.success(Unit) },
    undoActions: PodcastHomeUndoActions,
    removedFromUpNextMessage: String,
    modifier: Modifier = Modifier,
) {
    val progress by playbackProgressFlow.collectAsStateWithLifecycle()
    PodcastHomeUpNextDestination(
        episodes = episodes,
        podcastsById = podcastsById,
        playback = playback,
        playbackProgress = progress,
        downloadProgress = downloadProgress,
        downloadAssets = downloadAssets,
        reorderMode = reorderMode,
        filter = filter,
        onFilterChange = onFilterChange,
        onOpen = onOpen,
        onLongPress = onLongPress,
        onRemoveError = onRemoveError,
        onReorder = onReorder,
        onPlay = onPlay,
        onRemoveFromQueue = onRemoveFromQueue,
        onRestoreToQueue = onRestoreToQueue,
        onDownload = onDownload,
        onRetryDownload = onRetryDownload,
        onRemoveDownload = onRemoveDownload,
        undoActions = undoActions,
        removedFromUpNextMessage = removedFromUpNextMessage,
        modifier = modifier,
    )
}
