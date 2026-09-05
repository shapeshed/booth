package com.shapeshed.booth.ui

import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
 
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.shapeshed.booth.BoothApp
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadAssetStatus
import com.shapeshed.booth.data.DownloadAssetType
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.PodcastDownloadManager
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.PodcastRefreshInterval
import com.shapeshed.booth.data.PodcastRefreshNetwork
import com.shapeshed.booth.data.PodcastSubscriptionsViewMode
import com.shapeshed.booth.data.PodcastSearchProvider
import com.shapeshed.booth.data.relativeTime
import com.shapeshed.booth.data.PodcastRefreshWorker
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    onRemoveFailed: (EpisodeEntity) -> Unit,
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp + LocalPodcastMiniPlayerInset.current),
        verticalArrangement = Arrangement.spacedBy(PodcastListItemSpacing),
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
                        if (filter == QueueFilter.ALL) stringResource(R.string.up_next_empty) else stringResource(R.string.no_matching_episodes),
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
                positionOverride = if (episode.id == activeEpisodeId) activeProgress?.let { fraction ->
                    (episode.durationMs?.takeIf { it > 0L }?.times(fraction))?.toLong()
                } else null,
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
                                onRemoveFailed(episode)
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
                dragHandleModifier = if (reorderMode && filter == QueueFilter.ALL) Modifier
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
                                    val reordered = orderedEpisodes.toMutableList()
                                    val moved = reordered.removeAt(currentIndex)
                                    reordered.add(targetIndex.coerceIn(0, reordered.size), moved)
                                    orderedEpisodes = reordered
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
                    } else Modifier,
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
        positionalThreshold = { distance -> distance * SwipeToDismissThresholdFraction },
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
                    Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.remove_from_up_next), tint = MaterialTheme.colorScheme.onErrorContainer)
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
                .fillMaxWidth(),
            ) {
                PodcastActionListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        if (compact) {
                            PodcastArtwork(
                                episode.artworkUrl,
                                episode.title,
                                Modifier
                                    .size(PodcastEpisodeArtworkSize),
                            )
                        } else {
                            Row(
                                modifier = dragHandleModifier,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (showDragHandle) {
                                    Box(
                                        modifier = Modifier.width(32.dp).height(PodcastEpisodeArtworkSize).offset(x = (-8).dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Rounded.DragHandle,
                                            contentDescription = stringResource(R.string.long_press_drag_reorder, episode.title),
                                            tint = if (dragging) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                PodcastArtwork(
                                    episode.artworkUrl,
                                    episode.title,
                                    Modifier.size(PodcastEpisodeArtworkSize),
                                )
                            }
                        }
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                EpisodeTitleBlock(
                                    episode,
                                    active,
                                    downloaded = episode.localUri != null || downloadProgress?.completed == true,
                                    downloadProgress = downloadProgress,
                                )
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
                    },
                    trailingContent = if (compact && showDragHandle) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .then(dragHandleModifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.DragHandle,
                                    contentDescription = stringResource(R.string.move_episode, episode.title),
                                    tint = if (dragging) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    } else null,
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
        positionalThreshold = { distance -> distance * SwipeToDismissThresholdFraction },
    )
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAddToQueue()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                onDismiss()
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
                    contentDescription = stringResource(if (isAdding) R.string.add_to_up_next_accessibility else R.string.dismiss_episode),
                    tint = content,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (isAdding) R.string.podcast_up_next else R.string.dismiss), color = content, style = MaterialTheme.typography.labelLarge)
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
                            downloaded = episode.localUri != null || downloadProgress?.completed == true,
                            downloadProgress = downloadProgress,
                        )
                    },
                )
            }
        }
    }
}
