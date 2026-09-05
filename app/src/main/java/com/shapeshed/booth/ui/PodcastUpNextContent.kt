package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    onRemoveFailed: (EpisodeEntity) -> Unit,
    onReorder: (List<Long>) -> Unit,
    reorderMode: Boolean,
    onLongPress: (EpisodeEntity) -> Unit,
    filter: QueueFilter,
    onFilterChange: (QueueFilter) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onDownload: (EpisodeEntity) -> Unit,
    downloadProgress: Map<Long, DownloadProgress>,
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
        onRemoveFailed = onRemoveFailed,
        onReorder = onReorder,
        reorderMode = reorderMode,
        onLongPress = onLongPress,
        filter = filter,
        onFilterChange = onFilterChange,
        showFilterChips = false,
        onPlay = onPlay,
        onDownload = onDownload,
        downloadProgress = downloadProgress,
        modifier = modifier,
    )
}
