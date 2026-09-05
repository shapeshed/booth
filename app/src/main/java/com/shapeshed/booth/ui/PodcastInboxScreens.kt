package com.shapeshed.booth.ui

import androidx.compose.ui.res.stringResource
import androidx.paging.compose.LazyPagingItems
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    modifier: Modifier = Modifier,
) {
    val visibleEpisodes = episodes
    val listState = rememberLazyListState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp + LocalPodcastMiniPlayerInset.current),
            verticalArrangement = Arrangement.spacedBy(PodcastListItemSpacing),
        ) {
            if (visibleEpisodes.itemCount == 0 && visibleEpisodes.loadState.refresh is androidx.paging.LoadState.NotLoading) {
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
                    positionOverride = if (episode.id == activeEpisodeId) activeProgress?.let { fraction ->
                        (episode.durationMs?.takeIf { it > 0L }?.times(fraction))?.toLong()
                    } else null,
                    downloadProgress = downloadProgress[episode.id],
                    swipeEnabled = true,
                )
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
    onRemove: (EpisodeEntity) -> Unit,
    onLongPress: (EpisodeEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortOrder by rememberSaveable { mutableStateOf(DownloadsSortOrder.DATE_NEWEST) }
    var dateMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var sizeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var stateMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val episodeIds = remember(assets) { assets.map { it.episodeId }.distinct() }
    val visibleAssets = remember(assets, episodeIds, sortOrder) {
        val representatives = episodeIds.mapNotNull { episodeId ->
            val episodeAssets = assets.filter { it.episodeId == episodeId }
            episodeAssets.firstOrNull { it.assetType == DownloadAssetType.AUDIO }
                ?: episodeAssets.maxByOrNull { it.updatedAtMillis }
        }
        when (sortOrder) {
            DownloadsSortOrder.DATE_NEWEST -> representatives.sortedByDescending { it.createdAtMillis }
            DownloadsSortOrder.DATE_OLDEST -> representatives.sortedBy { it.createdAtMillis }
            DownloadsSortOrder.SIZE_LARGEST -> representatives.sortedByDescending { it.totalBytes ?: it.bytesDownloaded }
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
            top = 12.dp,
            bottom = 16.dp + LocalPodcastMiniPlayerInset.current,
        ),
        verticalArrangement = Arrangement.spacedBy(PodcastListItemSpacing),
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
                            label = { Text(if (sortOrder == DownloadsSortOrder.DATE_OLDEST) stringResource(R.string.oldest) else stringResource(R.string.newest)) },
                        )
                        DropdownMenu(
                            expanded = dateMenuExpanded,
                            onDismissRequest = { dateMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.newest_first)) },
                                onClick = { dateMenuExpanded = false; sortOrder = DownloadsSortOrder.DATE_NEWEST },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.oldest_first)) },
                                onClick = { dateMenuExpanded = false; sortOrder = DownloadsSortOrder.DATE_OLDEST },
                            )
                        }
                    }
                }
                item {
                    Box {
                        FilterChip(
                            selected = isSizeSort(),
                            onClick = { sizeMenuExpanded = true },
                            label = { Text(if (sortOrder == DownloadsSortOrder.SIZE_SMALLEST) stringResource(R.string.smallest) else stringResource(R.string.largest)) },
                        )
                        DropdownMenu(
                            expanded = sizeMenuExpanded,
                            onDismissRequest = { sizeMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.largest_first)) },
                                onClick = { sizeMenuExpanded = false; sortOrder = DownloadsSortOrder.SIZE_LARGEST },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.smallest_first)) },
                                onClick = { sizeMenuExpanded = false; sortOrder = DownloadsSortOrder.SIZE_SMALLEST },
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
                                onClick = { stateMenuExpanded = false; sortOrder = DownloadsSortOrder.STATE_DOWNLOADED },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.in_progress_first)) },
                                onClick = { stateMenuExpanded = false; sortOrder = DownloadsSortOrder.STATE_ACTIVE },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.failed_first)) },
                                onClick = { stateMenuExpanded = false; sortOrder = DownloadsSortOrder.STATE_FAILED },
                            )
                        }
                    }
                }
            }
        }
        item(key = "download-summary") {
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
            item(key = "empty-downloads") {
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
        items(visibleAssets, key = { "download-${it.episodeId}" }) { asset ->
            val episode = episodes[asset.episodeId] ?: return@items
            val episodeProgress = downloadProgress[episode.id]
            val isDownloaded = episode.localUri != null || asset.status == DownloadAssetStatus.COMPLETED
            val isCompleted = episode.completed
            val isActive = activeEpisodeId == episode.id
            val podcastTitle = podcastsById[episode.podcastId]?.title.orEmpty()
            var itemHeightPx by remember { mutableIntStateOf(0) }
            val artworkOffset = with(LocalDensity.current) {
                if (itemHeightPx == 0) (-8).dp
                else (8.dp.toPx() - (itemHeightPx - PodcastEpisodeArtworkSize.toPx()) / 2f).toDp()
            }
            val dismissState = rememberSwipeToDismissBoxState(
                positionalThreshold = { distance -> distance * SwipeToDismissThresholdFraction },
            )
            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    onRemove(episode)
                }
            }
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = true,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
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

@Composable
internal fun downloadStatusLabel(
    asset: DownloadAssetEntity,
    isDownloaded: Boolean,
    progress: DownloadProgress?,
): String = when {
    isDownloaded -> stringResource(R.string.downloaded)
    asset.status == DownloadAssetStatus.FAILED -> stringResource(R.string.download_failed)
    asset.status == DownloadAssetStatus.RETRYING -> stringResource(R.string.retrying)
    asset.status == DownloadAssetStatus.CANCELLED -> stringResource(R.string.cancelled)
    progress?.fraction == null && (progress?.bytesDownloaded ?: 0L) > 0L ->
        stringResource(R.string.downloaded_size, formatFileSize(progress?.bytesDownloaded ?: 0L))
    else -> stringResource(R.string.downloading)
}

@Composable
internal fun DownloadStatusIcon(
    asset: DownloadAssetEntity?,
    isDownloaded: Boolean,
    isActive: Boolean = asset?.status in PodcastDownloadManager.ACTIVE_STATUSES,
    contentDescription: String,
) {
    if (!isDownloaded && asset?.status != DownloadAssetStatus.FAILED && asset?.status != DownloadAssetStatus.CANCELLED) {
        return
    }
    val imageVector = when {
        isDownloaded -> Icons.Rounded.Check
        asset?.status == DownloadAssetStatus.FAILED -> Icons.Rounded.ErrorOutline
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
        DownloadAssetStatus.DOWNLOADING,
        DownloadAssetStatus.RETRYING -> 1
        DownloadAssetStatus.FAILED,
        DownloadAssetStatus.CANCELLED -> 2
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
