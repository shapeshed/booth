package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import kotlinx.coroutines.flow.StateFlow

/**
 * Adapts the inbox to its content.
 *
 * Takes the played position rather than the ViewModel it came from, so the collection stays scoped
 * to the subtree that draws it.
 */
@Composable
internal fun PodcastHomeInboxDestination(
    inbox: LazyPagingItems<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    playback: PlaybackUiState,
    playbackProgress: PlaybackProgress,
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
    restoredEpisodeId: Long? = null,
    onRestore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PodcastHomeInboxContent(
        inbox = inbox,
        podcastsById = podcastsById,
        playback = playback,
        playbackProgress = playbackProgress,
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
        restoredEpisodeId = restoredEpisodeId,
        onRestore = onRestore,
        modifier = modifier,
    )
}

/**
 * Collects the played position for the inbox subtree only.
 *
 * Twice a second, so it must not reach the home screen body. Same reasoning and same shape as
 * `ScopedPodcastHomeDiscoveryDestination`.
 */
@Composable
internal fun ScopedPodcastHomeInboxDestination(
    inbox: LazyPagingItems<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    playback: PlaybackUiState,
    playbackProgressFlow: StateFlow<PlaybackProgress>,
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
    restoredEpisodeId: Long? = null,
    onRestore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val progress by playbackProgressFlow.collectAsStateWithLifecycle()
    PodcastHomeInboxDestination(
        inbox = inbox,
        podcastsById = podcastsById,
        playback = playback,
        playbackProgress = progress,
        selectionMode = selectionMode,
        selectedEpisodeIds = selectedEpisodeIds,
        onSelectedEpisodeIdsChange = onSelectedEpisodeIdsChange,
        onOpen = onOpen,
        onAddToQueue = onAddToQueue,
        onDismiss = onDismiss,
        onActions = onActions,
        onPlay = onPlay,
        onDownload = onDownload,
        onRefresh = onRefresh,
        refreshing = refreshing,
        downloadProgress = downloadProgress,
        restoredEpisodeId = restoredEpisodeId,
        onRestore = onRestore,
        modifier = modifier,
    )
}
