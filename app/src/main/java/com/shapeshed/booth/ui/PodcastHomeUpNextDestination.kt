package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity

@Composable
internal fun PodcastHomeUpNextDestination(
    episodes: List<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    playback: PlaybackUiState,
    downloadProgress: Map<Long, DownloadProgress>,
    viewModel: PodcastViewModel,
    context: android.content.Context,
    reorderMode: Boolean,
    filter: QueueFilter,
    onFilterChange: (QueueFilter) -> Unit,
    onOpen: (EpisodeEntity) -> Unit,
    onLongPress: (EpisodeEntity) -> Unit,
    onRemoveFailed: (EpisodeEntity) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    PodcastUpNextContent(
        episodes = episodes,
        podcastsById = podcastsById,
        activeEpisodeId = playback.episode?.id,
        activeProgress = playback.durationMs.takeIf { it > 0L }?.let { duration ->
            (playback.positionMs.toFloat() / duration).coerceIn(0f, 1f)
        },
        isPlaying = playback.isPlaying,
        isBuffering = playback.isBuffering,
        onOpen = onOpen,
        onRemove = viewModel::removeFromQueueAwait,
        onRemoveFailed = onRemoveFailed,
        onReorder = onReorder,
        reorderMode = reorderMode,
        onLongPress = onLongPress,
        filter = filter,
        onFilterChange = onFilterChange,
        onPlay = onPlay,
        onDownload = { viewModel.download(context, it.id) },
        downloadProgress = downloadProgress,
        modifier = modifier,
    )
}
