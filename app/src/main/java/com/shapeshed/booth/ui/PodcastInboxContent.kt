package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity

@Composable
internal fun PodcastInboxContent(
    episodes: LazyPagingItems<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    activeEpisodeId: Long?,
    activeProgress: Float?,
    isBuffering: Boolean,
    onOpen: (EpisodeEntity) -> Unit,
    onAddToQueue: (EpisodeEntity) -> Unit,
    onDismiss: (EpisodeEntity) -> Unit,
    onActions: (EpisodeEntity) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onDownload: (EpisodeEntity) -> Unit,
    isPlaying: Boolean,
    onRefresh: () -> Unit,
    refreshing: Boolean,
    selectedEpisodeIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    downloadProgress: Map<Long, DownloadProgress>,
    modifier: Modifier = Modifier,
) {
    PodcastInbox(
        episodes = episodes,
        podcastsById = podcastsById,
        activeEpisodeId = activeEpisodeId,
        activeProgress = activeProgress,
        isBuffering = isBuffering,
        onOpen = onOpen,
        onAddToQueue = onAddToQueue,
        onDismiss = onDismiss,
        onActions = onActions,
        onPlay = onPlay,
        onDownload = onDownload,
        isPlaying = isPlaying,
        onRefresh = onRefresh,
        refreshing = refreshing,
        selectedEpisodeIds = selectedEpisodeIds,
        onToggleSelection = onToggleSelection,
        downloadProgress = downloadProgress,
        modifier = modifier,
    )
}
