package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity

@Composable
internal fun PodcastHomeInboxContent(
    inbox: LazyPagingItems<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    playback: PlaybackUiState,
    inboxSelectionMode: Boolean,
    selectedInboxIds: Set<Long>,
    onSelectedInboxIdsChange: (Set<Long>) -> Unit,
    onOpen: (EpisodeEntity) -> Unit,
    onAddToQueue: (EpisodeEntity) -> Unit,
    onDismiss: (EpisodeEntity) -> Unit,
    onActions: (EpisodeEntity) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onDownload: (EpisodeEntity) -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean,
    downloadProgress: Map<Long, DownloadProgress>,
    playbackProgress: PlaybackProgress,
    downloadAssets: Map<Long, DownloadAssetEntity> = emptyMap(),
    onRetryDownload: (episodeId: Long) -> Unit = {},
    onRemoveDownload: suspend (episodeId: Long) -> Result<Unit> = { Result.success(Unit) },
    restoredEpisodeId: Long? = null,
    onRestore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (inboxSelectionMode) {
            PodcastInboxSelectionHeader(
                inbox = inbox.itemSnapshotList.items,
                selectedIds = selectedInboxIds,
                onSelectedIdsChange = onSelectedInboxIdsChange,
            )
        }
        PodcastInboxContent(
            episodes = inbox,
            podcastsById = podcastsById,
            activeEpisodeId = playback.episode?.id,
            activeProgress = playbackProgress.fraction,
            isBuffering = playback.isBuffering,
            onOpen = onOpen,
            onAddToQueue = onAddToQueue,
            onDismiss = onDismiss,
            onActions = onActions,
            onPlay = onPlay,
            onDownload = onDownload,
            isPlaying = playback.isPlaying,
            selectedEpisodeIds = selectedInboxIds,
            onToggleSelection = { episodeId ->
                onSelectedInboxIdsChange(
                    if (episodeId in selectedInboxIds) {
                        selectedInboxIds - episodeId
                    } else {
                        selectedInboxIds + episodeId
                    },
                )
            },
            onRefresh = onRefresh,
            refreshing = refreshing,
            downloadProgress = downloadProgress,
            downloadAssets = downloadAssets,
            onRetryDownload = onRetryDownload,
            onRemoveDownload = onRemoveDownload,
            restoredEpisodeId = restoredEpisodeId,
            onRestore = onRestore,
            modifier = Modifier.weight(1f),
        )
    }
}
