package com.shapeshed.booth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.shapeshed.booth.R
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.isDownloaded
import kotlinx.coroutines.launch

@Composable
internal fun PodcastQueueScreen(
    episodes: List<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    activeEpisodeId: Long?,
    activeProgress: Float?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onOpen: (EpisodeEntity) -> Unit,
    onRemove: suspend (Long) -> Result<Unit>,
    onRemoveError: (EpisodeEntity) -> Unit,
    onReorder: (List<Long>) -> Unit,
    reorderMode: Boolean,
    onPlay: (EpisodeEntity) -> Unit,
    onDownload: (EpisodeEntity) -> Unit,
    onLongPress: (EpisodeEntity) -> Unit,
    downloadProgress: Map<Long, DownloadProgress>,
    filter: QueueFilter,
    onFilterChange: (QueueFilter) -> Unit,
    modifier: Modifier = Modifier,
    showFilterChips: Boolean = true,
) {
    val listState = rememberLazyListState()
    var orderedEpisodes by remember { mutableStateOf(episodes) }
    var pendingRemovalIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var removalBackups by remember { mutableStateOf<Map<Long, QueueRemoval<EpisodeEntity>>>(emptyMap()) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(episodes) {
        val confirmedRemovalIds = pendingRemovalIds.filter { id -> episodes.none { it.id == id } }.toSet()
        if (confirmedRemovalIds.isNotEmpty()) {
            pendingRemovalIds = pendingRemovalIds - confirmedRemovalIds
            removalBackups = removalBackups - confirmedRemovalIds
        }
        if (draggingId == null) {
            orderedEpisodes = episodes.filterNot { it.id in pendingRemovalIds }
        }
    }
    val visibleEpisodes = remember(orderedEpisodes, filter, pendingRemovalIds) {
        orderedEpisodes.filterNot { it.id in pendingRemovalIds }.filter { episode ->
            when (filter) {
                QueueFilter.ALL -> true
                QueueFilter.UNPLAYED -> !episode.completed && episode.positionMs <= 0L
                QueueFilter.IN_PROGRESS -> !episode.completed && episode.positionMs > 0L
                QueueFilter.COMPLETED -> episode.completed
            }
        }
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom =
                16.dp + LocalPodcastMiniPlayerInset.current,
        ),
        verticalArrangement = Arrangement.spacedBy(PODCAST_LIST_ITEM_SPACING),
    ) {
        if (showFilterChips) {
            item(key = "queue-filters") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QueueFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { onFilterChange(option) },
                            label = {
                                Text(
                                    when (option) {
                                        QueueFilter.ALL -> stringResource(R.string.up_next_all)
                                        QueueFilter.UNPLAYED -> stringResource(R.string.up_next_unplayed)
                                        QueueFilter.IN_PROGRESS -> stringResource(R.string.up_next_in_progress)
                                        QueueFilter.COMPLETED -> stringResource(R.string.up_next_completed)
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
        if (visibleEpisodes.isEmpty()) {
            item(key = "empty-queue") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (filter ==
                            QueueFilter.ALL
                        ) {
                            stringResource(R.string.up_next_empty)
                        } else {
                            stringResource(R.string.no_matching_episodes)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (filter == QueueFilter.ALL) {
                            stringResource(R.string.swipe_to_add_to_up_next)
                        } else {
                            stringResource(R.string.try_another_up_next_filter)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(visibleEpisodes, key = { "queue-${it.id}" }) { episode ->
            val podcastTitle = podcastsById[episode.podcastId]?.title.orEmpty()
            val isDragging = draggingId == episode.id
            QueueEpisodeSwipeRow(
                episode = episode,
                podcastTitle = podcastTitle,
                active = episode.id == activeEpisodeId,
                progressOverride = if (episode.id == activeEpisodeId) activeProgress else null,
                positionOverride = activeEpisodePositionOverride(
                    episodeId = episode.id,
                    activeEpisodeId = activeEpisodeId,
                    playedFraction = activeProgress,
                    episodeDurationMs = episode.durationMs,
                ),
                dragging = isDragging,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                downloadProgress = downloadProgress[episode.id],
                onOpen = { onOpen(episode) },
                onPlay = { onPlay(episode) },
                onDownload = { onDownload(episode) },
                onLongPress = { onLongPress(episode) },
                onRemove = {
                    if (episode.id !in pendingRemovalIds) {
                        val removal = removeQueueItem(orderedEpisodes, episode)
                        if (removal == null) {
                            Result.success(Unit)
                        } else {
                            orderedEpisodes = removal.first
                            pendingRemovalIds = pendingRemovalIds + episode.id
                            removalBackups = removalBackups + (episode.id to removal.second)
                            val result = onRemove(episode.id)
                            if (result.isFailure) {
                                removalBackups[episode.id]?.let { backup ->
                                    orderedEpisodes = restoreQueueItem(orderedEpisodes, backup)
                                }
                                pendingRemovalIds = pendingRemovalIds - episode.id
                                removalBackups = removalBackups - episode.id
                                onRemoveError(episode)
                            }
                            result
                        }
                    } else {
                        Result.success(Unit)
                    }
                },
                showDragHandle = reorderMode && filter == QueueFilter.ALL,
                compact = false,
                modifier = Modifier
                    .animateItem()
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) dragDistance else 0f },
                dragHandleModifier = if (reorderMode && filter == QueueFilter.ALL) {
                    Modifier
                        .pointerInput(episode.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = episode.id
                                    dragDistance = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (draggingId != episode.id) return@detectDragGesturesAfterLongPress
                                    dragDistance += dragAmount.y
                                    val currentIndex = orderedEpisodes.indexOfFirst { it.id == episode.id }
                                    val currentInfo = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == "queue-${episode.id}" }
                                        ?: return@detectDragGesturesAfterLongPress
                                    val center = currentInfo.offset + currentInfo.size / 2 + dragDistance
                                    val targetInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                        info.key.toString().startsWith("queue-") &&
                                            info.key != currentInfo.key &&
                                            center > info.offset &&
                                            center < info.offset + info.size
                                    }
                                    val targetId = targetInfo?.key
                                        ?.toString()
                                        ?.removePrefix("queue-")
                                        ?.toLongOrNull()
                                        ?: return@detectDragGesturesAfterLongPress
                                    val targetIndex = orderedEpisodes.indexOfFirst { it.id == targetId }
                                    if (targetIndex < 0) return@detectDragGesturesAfterLongPress
                                    if (targetIndex in orderedEpisodes.indices && targetIndex != currentIndex) {
                                        orderedEpisodes = reorderQueueItem(
                                            orderedEpisodes,
                                            episode,
                                            orderedEpisodes[targetIndex],
                                        )
                                        dragDistance -= (targetInfo.offset - currentInfo.offset).toFloat()
                                    }
                                },
                                onDragEnd = {
                                    onReorder(orderedEpisodes.map(EpisodeEntity::id))
                                    draggingId = null
                                    dragDistance = 0f
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragDistance = 0f
                                },
                            )
                        }
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
internal fun QueueEpisodeSwipeRow(
    episode: EpisodeEntity,
    podcastTitle: String,
    active: Boolean,
    dragging: Boolean,
    progressOverride: Float?,
    positionOverride: Long?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    downloadProgress: DownloadProgress?,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onLongPress: () -> Unit,
    onRemove: suspend () -> Result<Unit>,
    showDragHandle: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var showRemovalConfirmation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val queueEpisodeDescription = stringResource(R.string.queue_episode_description, episode.title, podcastTitle)
    val queueSwipeHint = stringResource(R.string.queue_swipe_hint)
    val selectedDescription = stringResource(R.string.selected_item)
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * SWIPE_TO_DISMISS_THRESHOLD_FRACTION },
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            showRemovalConfirmation = true
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.remove),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.remove_from_up_next),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
    ) {
        Column(Modifier.fillMaxWidth()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = when {
                    active -> MaterialTheme.colorScheme.surfaceContainerHigh
                    episode.completed -> MaterialTheme.colorScheme.surfaceContainerLow
                    else -> MaterialTheme.colorScheme.surface
                },
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
            ) {
                PodcastActionListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        PodcastArtwork(
                            episode.artworkUrl,
                            episode.title,
                            Modifier.size(PodcastEpisodeArtworkSize),
                        )
                    },
                    headlineContent = {
                        if (compact) {
                            Text(
                                text = episode.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
                            )
                        } else {
                            EpisodeTitleBlock(
                                episode,
                                active,
                                downloaded = episode.isDownloaded(downloadProgress),
                                downloadProgress = downloadProgress,
                            )
                        }
                    },
                    supportingContent = if (!compact) {
                        {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                EpisodeMetadataLine(episode, downloadProgress, active)
                                EpisodeActionRow(
                                    episode = episode,
                                    isPlaying = active && isPlaying,
                                    isBuffering = active && isBuffering,
                                    showPlayback = true,
                                    positionOverride = positionOverride,
                                    onPlay = onPlay,
                                    onActions = onLongPress,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    trailingContent = if (showDragHandle) {
                        {
                            IconButton(
                                onClick = {},
                                modifier = dragHandleModifier,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = stringResource(R.string.move_episode, episode.title),
                                    tint = if (dragging) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
    if (showRemovalConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemovalConfirmation = false },
            title = { Text(stringResource(R.string.remove_episode, episode.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemovalConfirmation = false
                        coroutineScope.launch { onRemove() }
                    },
                ) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemovalConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
internal fun InboxEpisodeSwipeRow(
    episode: EpisodeEntity,
    podcastTitle: String,
    active: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onAddToQueue: () -> Unit,
    onDismiss: () -> Unit,
    onLongPress: () -> Unit,
    onActions: () -> Unit,
    onToggleSelection: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionOverride: Long?,
    downloadProgress: DownloadProgress?,
    modifier: Modifier = Modifier,
    swipeEnabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val queueEpisodeDescription = stringResource(R.string.queue_episode_description, episode.title, podcastTitle)
    val queueSwipeHint = stringResource(R.string.queue_swipe_hint)
    val selectedDescription = stringResource(R.string.selected_item)
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * SWIPE_TO_DISMISS_THRESHOLD_FRACTION },
    )
    // Keyed only on the swipe state, so neither callback restarting the effect is wanted. Read
    // through updated state so both stay current.
    val currentOnAddToQueue by rememberUpdatedState(onAddToQueue)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnAddToQueue()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }

            SwipeToDismissBoxValue.EndToStart -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                currentOnDismiss()
            }

            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = swipeEnabled && !selectionMode,
        enableDismissFromEndToStart = swipeEnabled && !selectionMode,
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isAdding = direction == SwipeToDismissBoxValue.StartToEnd
            val container = if (isAdding) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
            val content = if (isAdding) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            }
            Row(
                modifier = Modifier.fillMaxSize().background(container).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isAdding) Arrangement.Start else Arrangement.End,
            ) {
                Icon(
                    imageVector = if (isAdding) Icons.AutoMirrored.Rounded.PlaylistAdd else Icons.Rounded.Delete,
                    contentDescription = stringResource(
                        if (isAdding) R.string.add_to_up_next_accessibility else R.string.dismiss_episode,
                    ),
                    tint = content,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(if (isAdding) R.string.podcast_up_next else R.string.dismiss),
                    color = content,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    ) {
        Column(Modifier.fillMaxWidth()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = when {
                    active -> MaterialTheme.colorScheme.surfaceContainerHigh
                    selected -> MaterialTheme.colorScheme.surfaceContainerHigh
                    episode.completed -> MaterialTheme.colorScheme.surfaceContainerLow
                    else -> MaterialTheme.colorScheme.surface
                },
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = if (selectionMode) onToggleSelection else onOpen,
                        onLongClick = onLongPress,
                    )
                    .semantics {
                        contentDescription = buildString {
                            append(queueEpisodeDescription)
                            if (selected) append(" ").append(selectedDescription).append(".")
                            if (!selectionMode) append(" ").append(queueSwipeHint)
                        }
                    },
            ) {
                PodcastActionListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(PodcastEpisodeArtworkSize)
                                .clip(RoundedCornerShape(12.dp)),
                        ) {
                            PodcastArtwork(
                                episode.artworkUrl,
                                episode.title,
                                Modifier.fillMaxSize(),
                            )
                            if (selectionMode && selected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = stringResource(R.string.selected_item),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            EpisodeMetadataLine(episode, downloadProgress, active)
                            EpisodeActionRow(
                                episode = episode,
                                isPlaying = active && isPlaying,
                                isBuffering = active && isBuffering,
                                showPlayback = true,
                                positionOverride = positionOverride,
                                onPlay = onPlay,
                                onActions = onActions,
                            )
                        }
                    },
                    headlineContent = {
                        EpisodeTitleBlock(
                            episode,
                            active,
                            downloaded = episode.isDownloaded(downloadProgress),
                            downloadProgress = downloadProgress,
                        )
                    },
                )
            }
        }
    }
}
