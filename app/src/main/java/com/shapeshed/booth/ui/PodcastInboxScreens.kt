package com.shapeshed.booth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.shapeshed.booth.R
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadAssetStatus
import com.shapeshed.booth.data.DownloadAssetType
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastDownloadManager
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.isDownloaded
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
internal fun PodcastInbox(
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
    /**
     * An episode an undo has just put back, whose row is revealed when Paging presents it.
     *
     * The restore is a flag flip, so the episode reappears at its sorted position. Do not scroll
     * the list for Undo; let the restored row animate into that position. [onRestore] is called
     * once Paging presents the episode, so the caller can clear the request and allow a second undo
     * of the same episode to animate again.
     */
    restoredEpisodeId: Long? = null,
    onRestore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val visibleEpisodes = episodes
    val listState = rememberLazyListState()
    var refreshWasActive by remember { mutableStateOf(false) }
    var restoreAnimationReady by remember(restoredEpisodeId) { mutableStateOf(false) }
    // Read through updated state so the effect only restarts when the id or presented list changes.
    val currentOnRestore by rememberUpdatedState(onRestore)
    LaunchedEffect(refreshing, visibleEpisodes.itemCount) {
        if (refreshing) {
            refreshWasActive = true
        } else if (refreshWasActive) {
            // A refresh prepends newly discovered episodes. Reset the viewport after the
            // refresh completes so the user immediately sees those episodes.
            refreshWasActive = false
            listState.animateScrollToItem(0)
        }
    }
    // The restored item may arrive in a refreshed PagingData with the same item count, so wait for
    // the item itself rather than using itemCount as a proxy. Keep it collapsed until then so the
    // top-edge reveal begins only after it has its final sorted position in the list.
    LaunchedEffect(restoredEpisodeId, visibleEpisodes) {
        val id = restoredEpisodeId ?: return@LaunchedEffect
        val restoredIndex = snapshotFlow {
            val snapshot = visibleEpisodes.itemSnapshotList
            snapshot.items.indexOfFirst { it.id == id }
                .takeIf { it >= 0 }
                ?.let { snapshot.placeholdersBefore + it }
                ?: -1
        }.first { it >= 0 }
        // LazyColumn can retain a stale first-visible index while the entering row is collapsed.
        // Explicitly align the first row before revealing it; other sorted positions stay put.
        if (restoredIndex == 0) {
            listState.animateScrollToItem(index = 0, scrollOffset = 0)
        }
        restoreAnimationReady = true
        currentOnRestore()
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom =
                    16.dp + LocalPodcastMiniPlayerInset.current,
            ),
            verticalArrangement = Arrangement.spacedBy(PODCAST_LIST_ITEM_SPACING),
        ) {
            if (visibleEpisodes.itemCount == 0) {
                item(key = "empty-inbox") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(R.string.inbox_clear),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.new_episodes_from_subscriptions),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(
                count = visibleEpisodes.itemCount,
                key = { index -> visibleEpisodes.peek(index)?.let { "inbox-${it.id}" } ?: "inbox-placeholder-$index" },
            ) { index ->
                val episode = visibleEpisodes[index] ?: return@items
                PodcastRestoreAnimation(
                    restored = episode.id == restoredEpisodeId,
                    startWhenReady = restoreAnimationReady,
                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                ) {
                    InboxEpisodeSwipeRow(
                        episode = episode,
                        podcastTitle = podcastsById[episode.podcastId]?.title.orEmpty(),
                        active = episode.id == activeEpisodeId,
                        selected = episode.id in selectedEpisodeIds,
                        selectionMode = selectedEpisodeIds.isNotEmpty(),
                        onOpen = { onOpen(episode) },
                        onAddToQueue = { onAddToQueue(episode) },
                        onDismiss = { onDismiss(episode) },
                        onLongPress = { onToggleSelection(episode.id) },
                        onActions = { onActions(episode) },
                        onToggleSelection = { onToggleSelection(episode.id) },
                        onPlay = { onPlay(episode) },
                        onDownload = { onDownload(episode) },
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        positionOverride = activeEpisodePositionOverride(
                            episodeId = episode.id,
                            activeEpisodeId = activeEpisodeId,
                            playedFraction = activeProgress,
                            episodeDurationMs = episode.durationMs,
                        ),
                        downloadProgress = downloadProgress[episode.id],
                        swipeEnabled = true,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PodcastDownloadsScreen(
    assets: List<DownloadAssetEntity>,
    episodes: Map<Long, EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    activeEpisodeId: Long?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    downloadProgress: Map<Long, DownloadProgress>,
    onOpen: (EpisodeEntity) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onDownload: (EpisodeEntity) -> Unit,
    onRemove: suspend (EpisodeEntity) -> Result<Unit>,
    onLongPress: (EpisodeEntity) -> Unit,
    undoActions: PodcastHomeUndoActions,
    pendingRemovalEpisodeIds: Set<Long>,
    onPendingRemovalEpisodeChange: (episodeId: Long, pending: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortOrder by rememberSaveable { mutableStateOf(DownloadsSortOrder.DATE_NEWEST) }
    var dateMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var sizeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var stateMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var restoredDownloadEpisodeId by remember { mutableStateOf<Long?>(null) }
    val currentOnPendingRemovalChange by rememberUpdatedState(onPendingRemovalEpisodeChange)
    // Keep the bytes and database rows until Undo expires, but hide the pending row immediately.
    // This state is deliberately not saveable: if the process dies before the snackbar resolves,
    // the untouched download should simply be visible again on the next launch.
    LaunchedEffect(assets) {
        val existingIds = assets.mapTo(mutableSetOf()) { it.episodeId }
        pendingRemovalEpisodeIds.filterNot { it in existingIds }
            .forEach { currentOnPendingRemovalChange(it, false) }
    }
    val episodeIds = remember(assets, pendingRemovalEpisodeIds) {
        assets.map { it.episodeId }.distinct().filterNot { it in pendingRemovalEpisodeIds }
    }
    val currentOnRemove by rememberUpdatedState(onRemove)
    val visibleAssets = remember(assets, episodeIds, sortOrder) {
        val representatives = episodeIds.mapNotNull { episodeId ->
            val episodeAssets = assets.filter { it.episodeId == episodeId }
            episodeAssets.firstOrNull { it.assetType == DownloadAssetType.AUDIO }
                ?: episodeAssets.maxByOrNull { it.updatedAtMillis }
        }
        when (sortOrder) {
            DownloadsSortOrder.DATE_NEWEST -> representatives.sortedByDescending { it.createdAtMillis }

            DownloadsSortOrder.DATE_OLDEST -> representatives.sortedBy { it.createdAtMillis }

            DownloadsSortOrder.SIZE_LARGEST -> representatives.sortedByDescending {
                it.totalBytes ?: it.bytesDownloaded
            }

            DownloadsSortOrder.SIZE_SMALLEST -> representatives.sortedBy { it.totalBytes ?: it.bytesDownloaded }

            DownloadsSortOrder.STATE_DOWNLOADED -> representatives.sortedWith(
                compareBy<DownloadAssetEntity> { downloadStateRank(it, DownloadsSortOrder.STATE_DOWNLOADED) }
                    .thenByDescending { it.updatedAtMillis },
            )

            DownloadsSortOrder.STATE_ACTIVE -> representatives.sortedWith(
                compareBy<DownloadAssetEntity> { downloadStateRank(it, DownloadsSortOrder.STATE_ACTIVE) }
                    .thenByDescending { it.updatedAtMillis },
            )

            DownloadsSortOrder.STATE_FAILED -> representatives.sortedWith(
                compareBy<DownloadAssetEntity> { downloadStateRank(it, DownloadsSortOrder.STATE_FAILED) }
                    .thenByDescending { it.updatedAtMillis },
            )
        }
    }

    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    fun isDateSort() = sortOrder == DownloadsSortOrder.DATE_NEWEST || sortOrder == DownloadsSortOrder.DATE_OLDEST

    fun isSizeSort() = sortOrder == DownloadsSortOrder.SIZE_LARGEST ||
        sortOrder == DownloadsSortOrder.SIZE_SMALLEST

    fun isStateSort() = sortOrder == DownloadsSortOrder.STATE_DOWNLOADED ||
        sortOrder == DownloadsSortOrder.STATE_ACTIVE ||
        sortOrder == DownloadsSortOrder.STATE_FAILED

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 16.dp + LocalPodcastMiniPlayerInset.current,
        ),
        verticalArrangement = Arrangement.spacedBy(PODCAST_LIST_ITEM_SPACING),
    ) {
        item(key = "download-filters") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Box {
                        FilterChip(
                            selected = isDateSort(),
                            onClick = { dateMenuExpanded = true },
                            label = {
                                Text(
                                    if (sortOrder ==
                                        DownloadsSortOrder.DATE_OLDEST
                                    ) {
                                        stringResource(R.string.oldest)
                                    } else {
                                        stringResource(R.string.newest)
                                    },
                                )
                            },
                        )
                        DropdownMenu(
                            expanded = dateMenuExpanded,
                            onDismissRequest = { dateMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.newest_first)) },
                                onClick = {
                                    dateMenuExpanded = false
                                    sortOrder = DownloadsSortOrder.DATE_NEWEST
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.oldest_first)) },
                                onClick = {
                                    dateMenuExpanded = false
                                    sortOrder = DownloadsSortOrder.DATE_OLDEST
                                },
                            )
                        }
                    }
                }
                item {
                    Box {
                        FilterChip(
                            selected = isSizeSort(),
                            onClick = { sizeMenuExpanded = true },
                            label = {
                                Text(
                                    if (sortOrder ==
                                        DownloadsSortOrder.SIZE_SMALLEST
                                    ) {
                                        stringResource(R.string.smallest)
                                    } else {
                                        stringResource(R.string.largest)
                                    },
                                )
                            },
                        )
                        DropdownMenu(
                            expanded = sizeMenuExpanded,
                            onDismissRequest = { sizeMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.largest_first)) },
                                onClick = {
                                    sizeMenuExpanded = false
                                    sortOrder = DownloadsSortOrder.SIZE_LARGEST
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.smallest_first)) },
                                onClick = {
                                    sizeMenuExpanded = false
                                    sortOrder = DownloadsSortOrder.SIZE_SMALLEST
                                },
                            )
                        }
                    }
                }
                item {
                    Box {
                        FilterChip(
                            selected = isStateSort(),
                            onClick = { stateMenuExpanded = true },
                            label = {
                                Text(
                                    when (sortOrder) {
                                        DownloadsSortOrder.STATE_ACTIVE -> stringResource(R.string.in_progress_first)
                                        DownloadsSortOrder.STATE_FAILED -> stringResource(R.string.failed)
                                        else -> stringResource(R.string.downloaded)
                                    },
                                )
                            },
                        )
                        DropdownMenu(
                            expanded = stateMenuExpanded,
                            onDismissRequest = { stateMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.downloaded_first)) },
                                onClick = {
                                    stateMenuExpanded = false
                                    sortOrder = DownloadsSortOrder.STATE_DOWNLOADED
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.in_progress_first)) },
                                onClick = {
                                    stateMenuExpanded = false
                                    sortOrder = DownloadsSortOrder.STATE_ACTIVE
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.failed_first)) },
                                onClick = {
                                    stateMenuExpanded = false
                                    sortOrder = DownloadsSortOrder.STATE_FAILED
                                },
                            )
                        }
                    }
                }
            }
        }
        item(key = "download-summary", contentType = "summary") {
            val knownBytes = assets.sumOf { it.totalBytes ?: 0L }
            val downloadedBytes = assets.sumOf { it.bytesDownloaded }
            val sizeLabel = when {
                knownBytes > 0L -> formatFileSize(knownBytes)
                downloadedBytes > 0L -> stringResource(R.string.downloaded_size, formatFileSize(downloadedBytes))
                else -> "0 B"
            }
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (visibleAssets.isEmpty()) {
            item(key = "empty-downloads", contentType = "status") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_downloads), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.downloaded_episodes_offline),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(visibleAssets, key = { "download-${it.episodeId}" }, contentType = { "download" }) { asset ->
            val episode = episodes[asset.episodeId] ?: return@items
            val episodeProgress = downloadProgress[episode.id]
            // The asset row is authoritative for this list, and the live progress covers the
            // window between a transfer completing and that row being written.
            val isDownloaded = episode.isDownloaded(episodeProgress) ||
                asset.status == DownloadAssetStatus.COMPLETED
            val isCompleted = episode.completed
            val isActive = activeEpisodeId == episode.id
            val podcastTitle = podcastsById[episode.podcastId]?.title.orEmpty()
            var itemHeightPx by remember { mutableIntStateOf(0) }
            val artworkOffset = with(LocalDensity.current) {
                if (itemHeightPx == 0) {
                    (-8).dp
                } else {
                    (8.dp.toPx() - (itemHeightPx - PodcastEpisodeArtworkSize.toPx()) / 2f).toDp()
                }
            }
            val dismissState = rememberRowSwipeDismissState()
            // Read through updated state so the one-shot callback always uses the current row's
            // id, since a lazy item's captured lambda goes stale as the list is recycled.
            val currentEpisodeId by rememberUpdatedState(episode.id)
            // This row is removed from the LazyColumn immediately while Undo is offered. Do not
            // animate its dismiss state after removal: that would keep measuring a detached node.
            val rowScope = rememberCoroutineScope()
            val actedOn = rememberSwipeDismissGuard()
            PodcastRestoreAnimation(
                restored = asset.episodeId == restoredDownloadEpisodeId,
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
            ) {
                SwipeToDismissBox(
                    state = dismissState,
                    onDismiss = { value ->
                        when (decideSwipeDismissAction(value, actedOn.intValue)) {
                            SwipeDismissAction.Act -> {
                                actedOn.intValue = value.ordinal
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val episodeId = currentEpisodeId
                                currentOnPendingRemovalChange(episodeId, true)
                                undoActions.removeDownloadWithUndo(
                                    undo = {
                                        currentOnPendingRemovalChange(episodeId, false)
                                        restoredDownloadEpisodeId = episodeId
                                    },
                                    commit = {
                                        currentOnRemove(episode).also { result ->
                                            if (result.isFailure) {
                                                currentOnPendingRemovalChange(episodeId, false)
                                            }
                                        }
                                    },
                                )
                            }

                            SwipeDismissAction.SettleAndRearm -> rowScope.launch {
                                dismissState.reset()
                                actedOn.intValue = NO_DIRECTION_ACTED_ON
                            }

                            SwipeDismissAction.Ignore -> Unit
                        }
                    },
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .testTag(SWIPE_ROW_TEST_TAG),
                    backgroundContent = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.remove_episode, episode.title),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    },
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else if (isCompleted) {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { itemHeightPx = it.height }
                            .combinedClickable(
                                onClick = { onOpen(episode) },
                                onLongClick = { onLongPress(episode) },
                            ),
                    ) {
                        Column {
                            PodcastActionListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    PodcastArtwork(
                                        imageUrl = episode.artworkUrl ?: podcastsById[episode.podcastId]?.artworkUrl,
                                        title = episode.title,
                                        modifier = Modifier
                                            .size(PodcastEpisodeArtworkSize)
                                            .offset(y = 0.dp),
                                    )
                                },
                                headlineContent = {
                                    EpisodeTitleBlock(
                                        episode,
                                        isActive,
                                        downloaded = isDownloaded,
                                        downloadProgress = episodeProgress,
                                    )
                                },
                                supportingContent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        DownloadEpisodeMetadataLine(
                                            episode = episode,
                                            podcastTitle = podcastTitle,
                                            asset = asset,
                                            progress = episodeProgress,
                                            active = isActive,
                                            downloaded = isDownloaded,
                                            completed = isCompleted,
                                        )
                                        EpisodeActionRow(
                                            episode = episode,
                                            isPlaying = isActive && isPlaying,
                                            isBuffering = isActive && isBuffering,
                                            showPlayback = isDownloaded,
                                            onPlay = { onPlay(episode) },
                                            onActions = { onLongPress(episode) },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun downloadStatusLabel(
    asset: DownloadAssetEntity,
    isDownloaded: Boolean,
    progress: DownloadProgress?,
): String = when {
    isDownloaded -> stringResource(R.string.downloaded)

    asset.status == DownloadAssetStatus.FAILED -> stringResource(R.string.download_failed)

    asset.status == DownloadAssetStatus.RETRYING -> stringResource(R.string.retrying)

    asset.status == DownloadAssetStatus.WAITING_FOR_WIFI -> stringResource(R.string.download_waiting_for_wifi)

    asset.status == DownloadAssetStatus.CANCELLED -> stringResource(R.string.cancelled)

    progress?.fraction == null && (progress?.bytesDownloaded ?: 0L) > 0L ->
        stringResource(R.string.downloaded_size, formatFileSize(progress?.bytesDownloaded ?: 0L))

    else -> stringResource(R.string.downloading)
}

@Composable
internal fun DownloadStatusIcon(
    asset: DownloadAssetEntity?,
    isDownloaded: Boolean,
    contentDescription: String,
    isActive: Boolean = asset?.status in PodcastDownloadManager.ACTIVE_STATUSES,
) {
    if (!isDownloaded && asset?.status != DownloadAssetStatus.FAILED &&
        asset?.status != DownloadAssetStatus.WAITING_FOR_WIFI &&
        asset?.status != DownloadAssetStatus.CANCELLED
    ) {
        return
    }
    val imageVector = when {
        isDownloaded -> Icons.Rounded.Check
        asset?.status == DownloadAssetStatus.FAILED -> Icons.Rounded.ErrorOutline
        asset?.status == DownloadAssetStatus.WAITING_FOR_WIFI -> Icons.Rounded.WifiOff
        asset?.status == DownloadAssetStatus.CANCELLED -> Icons.Rounded.Block
        else -> Icons.Rounded.FileDownload
    }
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = when {
            isDownloaded -> MaterialTheme.colorScheme.primary
            asset?.status == DownloadAssetStatus.FAILED -> MaterialTheme.colorScheme.error
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.primary
        },
        modifier = Modifier.size(16.dp),
    )
}

internal fun downloadSizeLabel(
    asset: DownloadAssetEntity,
    episode: EpisodeEntity,
    progress: DownloadProgress?,
): String? {
    val total = asset.totalBytes ?: episode.audioSizeBytes
    return when {
        asset.status in PodcastDownloadManager.ACTIVE_STATUSES && total != null && total > 0L ->
            "${formatFileSize(progress?.bytesDownloaded ?: 0L)} / ${formatFileSize(total)}"

        total != null && total > 0L -> formatFileSize(total)

        progress != null && progress.bytesDownloaded > 0L -> "${formatFileSize(progress.bytesDownloaded)} downloaded"

        else -> null
    }
}

internal fun downloadStateRank(asset: DownloadAssetEntity, preferred: DownloadsSortOrder): Int {
    val state = when (asset.status) {
        DownloadAssetStatus.COMPLETED -> 0

        DownloadAssetStatus.QUEUED,
        DownloadAssetStatus.WAITING_FOR_WIFI,
        DownloadAssetStatus.DOWNLOADING,
        DownloadAssetStatus.RETRYING,
        -> 1

        DownloadAssetStatus.FAILED,
        DownloadAssetStatus.CANCELLED,
        -> 2
    }
    return when (preferred) {
        DownloadsSortOrder.STATE_DOWNLOADED -> state

        DownloadsSortOrder.STATE_ACTIVE -> when (state) {
            1 -> 0
            0 -> 1
            else -> 2
        }

        DownloadsSortOrder.STATE_FAILED -> when (state) {
            2 -> 0
            1 -> 1
            else -> 2
        }

        else -> 0
    }
}

internal fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000L -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
