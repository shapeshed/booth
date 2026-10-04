package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity

@Composable
internal fun PodcastUpNextContent(
    episodes: List<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    activeEpisodeId: Long?,
    activeProgress: Float?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onOpen: (EpisodeEntity) -> Unit,
    onRemove: suspend (Long) -> Result<Unit>,
    onRestoreToQueue: suspend (episodeId: Long, position: Int) -> Result<Unit>,
    onRemoveError: (EpisodeEntity) -> Unit,
    onReorder: (List<Long>) -> Unit,
    reorderMode: Boolean,
    onLongPress: (EpisodeEntity) -> Unit,
    filter: QueueFilter,
    onFilterChange: (QueueFilter) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onDownload: (EpisodeEntity) -> Unit,
    downloadProgress: Map<Long, DownloadProgress>,
    downloadAssets: Map<Long, DownloadAssetEntity> = emptyMap(),
    undoActions: PodcastHomeUndoActions,
    removedFromUpNextMessage: String,
    onRetryDownload: (episodeId: Long) -> Unit = {},
    onRemoveDownload: suspend (episodeId: Long) -> Result<Unit> = { Result.success(Unit) },
    modifier: Modifier = Modifier,
) {
    PodcastQueueScreen(
        episodes = episodes,
        podcastsById = podcastsById,
        activeEpisodeId = activeEpisodeId,
        activeProgress = activeProgress,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        onOpen = onOpen,
        onRemove = onRemove,
        onRestoreToQueue = onRestoreToQueue,
        onRemoveError = onRemoveError,
        onReorder = onReorder,
        reorderMode = reorderMode,
        onLongPress = onLongPress,
        filter = filter,
        onFilterChange = onFilterChange,
        showFilterChips = false,
        onPlay = onPlay,
        onDownload = onDownload,
        downloadProgress = downloadProgress,
        downloadAssets = downloadAssets,
        undoActions = undoActions,
        removedFromUpNextMessage = removedFromUpNextMessage,
        onRetryDownload = onRetryDownload,
        onRemoveDownload = onRemoveDownload,
        modifier = modifier,
    )
}
