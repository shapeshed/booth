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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
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
import com.shapeshed.booth.data.displayCategories
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
@OptIn(ExperimentalMaterial3Api::class)
internal fun PodcastDetail(
    podcast: PodcastEntity,
    episodes: List<EpisodeEntity>,
    onRefresh: () -> Unit,
    refreshing: Boolean,
    onPlay: (EpisodeEntity) -> Unit,
    activeEpisodeId: Long?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    onTogglePlayPause: () -> Unit,
    showDescription: Boolean,
    onShowDescriptionChange: (Boolean) -> Unit,
    onDownload: (EpisodeEntity) -> Unit,
    isSubscribed: Boolean,
    onUnsubscribe: () -> Unit,
    onSubscribe: (com.shapeshed.booth.data.PodcastSearchResult) -> Unit,
    onTag: (String) -> Unit,
    onEpisodeClick: (EpisodeEntity) -> Unit,
    onEpisodeLongPress: (EpisodeEntity) -> Unit,
    downloadProgress: Map<Long, DownloadProgress>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var visibleEpisodeCount by rememberSaveable(podcast.id) { mutableIntStateOf(24) }
    val linkColor = MaterialTheme.colorScheme.primary
    val descriptionBlocks = remember(podcast.descriptionHtml, linkColor) {
        podcast.descriptionHtml?.let { formatDescriptionBlocks(it, linkColor) }.orEmpty()
    }
    val descriptionPreview = remember(descriptionBlocks) {
        descriptionBlocks
            .filterIsInstance<DescriptionBlock.TextBlock>()
            .joinToString(" ") { it.text.text.trim() }
            .trim()
            .takeIf(String::isNotBlank)
    }
    var showArtworkViewer by rememberSaveable(podcast.id) { mutableStateOf(false) }
    LaunchedEffect(podcast.id, episodes.size) {
        if (episodes.isNotEmpty()) {
            visibleEpisodeCount = visibleEpisodeCount.coerceAtMost(episodes.size)
        }
    }
    LaunchedEffect(podcast.id, episodes.size) {
        snapshotFlow {
            Triple(
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
                visibleEpisodeCount,
                episodes.size,
            )
        }.collect { (lastVisibleIndex, loadedCount, totalCount) ->
            val episodeStartIndex = 2 // podcast header and spacer
            val loadMoreThreshold = episodeStartIndex + loadedCount - 5
            if (lastVisibleIndex >= loadMoreThreshold && loadedCount < totalCount) {
                visibleEpisodeCount = (loadedCount + 24).coerceAtMost(totalCount)
            }
        }
    }
    PodcastDetailTemplate(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = listState,
        modifier = modifier,
    ) {
        item(key = "podcast-header") {
            Surface(color = MaterialTheme.colorScheme.surface) {
                PodcastDetailHeader(
                title = podcast.title,
                artworkUrl = podcast.artworkUrl,
                onArtworkClick = podcast.artworkUrl?.takeIf(String::isNotBlank)?.let { { showArtworkViewer = true } },
                author = podcast.author,
                description = descriptionPreview,
                onDescriptionClick = { onShowDescriptionChange(true) },
                categories = podcast.displayCategories(),
                // Saved tags are user metadata, not provider categories.
                onCategory = onTag,
                isSubscribed = isSubscribed,
                onSubscription = if (isSubscribed) {
                    onUnsubscribe
                } else {
                    {
                        onSubscribe(
                            com.shapeshed.booth.data.PodcastSearchResult(
                                providerId = "local",
                                podcast = com.shapeshed.booth.data.Podcast(
                                    id = podcast.id,
                                    title = podcast.title,
                                    author = podcast.author,
                                    feedUrl = podcast.feedUrl,
                                    siteUrl = podcast.siteUrl,
                                    descriptionHtml = podcast.descriptionHtml,
                                    artworkUrl = podcast.artworkUrl,
                                    explicit = podcast.explicit,
                                ),
                            ),
                        )
                    }
                },
                showSubscriptionAction = false,
                latestAction = { actionModifier ->
                    episodes.firstOrNull()?.let { latestEpisode ->
                        PodcastLatestEpisodeButton(
                        modifier = actionModifier,
                        durationMs = latestEpisode.durationMs,
                        positionMs = if (latestEpisode.id == activeEpisodeId) {
                            positionMs
                        } else {
                            latestEpisode.positionMs
                        },
                        completed = latestEpisode.completed,
                        isPlaying = latestEpisode.id == activeEpisodeId && isPlaying,
                        isBuffering = latestEpisode.id == activeEpisodeId && isBuffering,
                        onClick = {
                            if (latestEpisode.id == activeEpisodeId) onTogglePlayPause()
                            else onPlay(latestEpisode)
                        },
                        )
                    }
                },
                )
            }
        }
        if (episodes.isEmpty()) {
            item(key = "no-episodes") {
                Text(stringResource(R.string.no_episodes_available_yet), modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(episodes.take(visibleEpisodeCount), key = { it.id }) { episode ->
            EpisodeRow(
                episode = episode,
                onOpen = { onEpisodeClick(episode) },
                onLongPress = { onEpisodeLongPress(episode) },
                onPlay = { onPlay(episode) },
                onTogglePlayPause = onTogglePlayPause,
                active = episode.id == activeEpisodeId,
                isPlaying = isPlaying,
                isBuffering = episode.id == activeEpisodeId && isBuffering,
                positionOverride = episode.id.takeIf { it == activeEpisodeId }?.let { positionMs },
                downloaded = episode.localUri != null || downloadProgress[episode.id]?.completed == true,
                onDownload = { onDownload(episode) },
                downloadProgress = downloadProgress[episode.id],
            )
        }
    }
    if (showDescription && descriptionBlocks.isNotEmpty()) {
        val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
        LaunchedEffect(sheetState) { sheetState.expand() }
        ModalBottomSheet(
            onDismissRequest = { onShowDescriptionChange(false) },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.about_this_podcast), style = MaterialTheme.typography.headlineSmall)
                descriptionBlocks.forEach { block -> DescriptionBlockContent(block) }
            }
        }
    }
    if (showArtworkViewer && !podcast.artworkUrl.isNullOrBlank()) {
        PodcastArtworkViewer(
            imageUrl = podcast.artworkUrl,
            title = podcast.title,
            onDismiss = { showArtworkViewer = false },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PodcastEpisodeActionsSheet(
    action: PodcastEpisodeAction,
    downloadSizeBytes: Long? = action.downloadSizeBytes,
    loadingDownloadSize: Boolean = false,
    onShare: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: (() -> Unit)?,
    onToggleQueue: () -> Unit,
    onReorder: (() -> Unit)?,
    onSetPlayed: (Boolean) -> Unit,
    onResetPosition: () -> Unit,
    onDismiss: () -> Unit,
) {
    val episodeTitle = when (action) {
        is PodcastEpisodeAction.Subscribed -> action.episode.title
        is PodcastEpisodeAction.Preview -> action.episode.title
    }
    val podcastTitle = when (action) {
        is PodcastEpisodeAction.Subscribed -> action.podcastTitle
        is PodcastEpisodeAction.Preview -> action.podcastTitle
    }
    val podcastArtworkUrl = when (action) {
        is PodcastEpisodeAction.Subscribed -> action.podcastArtworkUrl
        is PodcastEpisodeAction.Preview -> action.podcastArtworkUrl
    }
    val linkUrl = action.linkUrl
    val downloadLabel = downloadSizeBytes?.takeIf { it > 0L }?.let {
        stringResource(R.string.download_with_size, formatFileSize(it))
    } ?: stringResource(R.string.download)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PodcastArtwork(
                    imageUrl = podcastArtworkUrl,
                    title = podcastTitle,
                    modifier = Modifier.size(64.dp),
                )
                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                    Text(
                        text = episodeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = podcastTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!action.isDownloaded) {
                    PodcastActionListItem(
                        headlineContent = { Text(downloadLabel, style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(24.dp))
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        trailingContent = if (loadingDownloadSize) {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDownload),
                    )
                } else {
                    PodcastActionListItem(
                        headlineContent = { Text(stringResource(R.string.remove_download), style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(24.dp))
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRemoveDownload?.invoke() },
                    )
                }
                onReorder?.let { reorder ->
                    PodcastActionListItem(
                        headlineContent = {
                            Text(stringResource(R.string.reorder), style = MaterialTheme.typography.bodyLarge)
                        },
                        leadingContent = {
                            Icon(Icons.Rounded.DragHandle, contentDescription = null, modifier = Modifier.size(24.dp))
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = reorder),
                    )
                }
                PodcastActionListItem(
                    headlineContent = {
                        Text(
                            if (action.isCompleted) stringResource(R.string.mark_unplayed_action) else stringResource(R.string.mark_played_action),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    leadingContent = {
                        Icon(
                            if (action.isCompleted) Icons.Rounded.Replay10 else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onSetPlayed(!action.isCompleted) }),
                )
                if (action.hasPlaybackPosition) {
                    PodcastActionListItem(
                        headlineContent = { Text(stringResource(R.string.reset_playback_position), style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = {
                            Icon(Icons.Rounded.Replay10, contentDescription = null, modifier = Modifier.size(24.dp))
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onResetPosition),
                    )
                }
                PodcastActionListItem(
                    headlineContent = {
                        Text(
                            if (action.isInQueue) stringResource(R.string.remove_from_up_next) else stringResource(R.string.add_to_up_next),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    leadingContent = {
                        Icon(
                            if (action.isInQueue) Icons.Rounded.Delete else Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleQueue),
                )
                if (!linkUrl.isNullOrBlank()) {
                        PodcastActionListItem(
                            headlineContent = { Text(stringResource(R.string.share), style = MaterialTheme.typography.bodyLarge) },
                            leadingContent = {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(24.dp))
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onShare),
                    )
                    PodcastActionListItem(
                        headlineContent = { Text(stringResource(R.string.open_in_browser), style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenInBrowser),
                    )
                }
            }
        }
    }
}
