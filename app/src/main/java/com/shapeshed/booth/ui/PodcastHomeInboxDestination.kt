package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity

@Composable
internal fun PodcastHomeInboxDestination(
    inbox: LazyPagingItems<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    playback: PlaybackUiState,
    selectionMode: Boolean,
    selectedEpisodeIds: Set<Long>,
    onSelectedEpisodeIdsChange: (Set<Long>) -> Unit,
    onOpen: (EpisodeEntity) -> Unit,
    onAddToQueue: (EpisodeEntity) -> Unit,
    onDismiss: (EpisodeEntity) -> Unit,
    onActions: (EpisodeEntity) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onDownload: (EpisodeEntity) -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean,
    downloadProgress: Map<Long, DownloadProgress>,
    modifier: Modifier = Modifier,
) {
    PodcastHomeInboxContent(
        inbox = inbox,
        podcastsById = podcastsById,
        playback = playback,
        inboxSelectionMode = selectionMode,
        selectedInboxIds = selectedEpisodeIds,
        onSelectedInboxIdsChange = onSelectedEpisodeIdsChange,
        onOpen = onOpen,
        onAddToQueue = onAddToQueue,
        onDismiss = onDismiss,
        onActions = onActions,
        onPlay = onPlay,
        onDownload = onDownload,
        onRefresh = onRefresh,
        refreshing = refreshing,
        downloadProgress = downloadProgress,
        modifier = modifier,
    )
}
