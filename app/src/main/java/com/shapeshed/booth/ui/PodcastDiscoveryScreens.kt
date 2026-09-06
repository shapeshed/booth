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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
internal fun PodcastCategoryListScreen(
    category: com.shapeshed.booth.data.PodcastDiscoveryShelfResult,
    onBrowse: (com.shapeshed.booth.data.PodcastSearchResult) -> Unit,
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, category.results.size, isLoadingMore) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisibleIndex ->
                if (category.results.size > 0 &&
                    lastVisibleIndex >= category.results.size - 6 &&
                    !isLoadingMore && hasMore
                ) {
                    onLoadMore()
                }
            }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = modifier,
        state = gridState,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = 16.dp + LocalPodcastMiniPlayerInset.current,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        gridItems(
            category.results,
            key = { "category-${it.podcast.id}" },
            contentType = { "podcast" },
        ) { result ->
            CategoryPodcastCard(result, onBrowse)
        }
        if (isLoadingMore) {
            item(
                span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
                contentType = "loading",
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
internal fun CategoryPodcastCard(
    result: com.shapeshed.booth.data.PodcastSearchResult,
    onBrowse: (com.shapeshed.booth.data.PodcastSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    PodcastGridCard(
        artworkUrl = result.podcast.artworkUrl,
        title = result.podcast.title,
        onClick = { onBrowse(result) },
        modifier = modifier,
    )
}

@Composable
internal fun PodcastGridCard(
    artworkUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = title },
    ) {
        Column {
            PodcastArtwork(
                imageUrl = artworkUrl,
                title = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .aspectRatio(1f),
            )
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PodcastDiscoveryPreview(
    result: com.shapeshed.booth.data.PodcastSearchResult,
    feed: com.shapeshed.booth.data.PodcastFeed?,
    isLoading: Boolean,
    error: PodcastUiError?,
    onSubscribe: () -> Unit,
    onDescriptionClick: () -> Unit,
    onUnsubscribe: () -> Unit,
    onCategory: (com.shapeshed.booth.data.PodcastDiscoveryCategory) -> Unit,
    onEpisodeClick: (com.shapeshed.booth.data.Episode) -> Unit,
    onLongPress: (com.shapeshed.booth.data.Episode) -> Unit,
    activeEpisodeId: Long?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    playbackProgress: Float?,
    onPlay: (com.shapeshed.booth.data.Episode) -> Unit,
    onDownload: (com.shapeshed.booth.data.Episode) -> Unit,
    downloadProgress: Map<Long, DownloadProgress>,
    savedEpisodes: Map<Long, EpisodeEntity>,
    isSubscribed: Boolean,
    modifier: Modifier = Modifier,
) {
    val descriptionHtml = feed?.podcast?.descriptionHtml ?: result.podcast.descriptionHtml
    val linkColor = MaterialTheme.colorScheme.primary
    val descriptionPreview = remember(descriptionHtml, linkColor) {
        descriptionHtml
            ?.let { formatDescriptionBlocks(it, linkColor) }
            .orEmpty()
            .filterIsInstance<DescriptionBlock.TextBlock>()
            .joinToString(" ") { it.text.text.trim() }
            .trim()
            .takeIf(String::isNotBlank)
    }
    var showArtworkViewer by rememberSaveable(result.podcast.id) { mutableStateOf(false) }
    PodcastDetailTemplate(
        refreshing = false,
        onRefresh = null,
        modifier = modifier,
    ) {
        item(key = "preview-header") {
            Surface(color = MaterialTheme.colorScheme.surface) {
                PodcastDetailHeader(
                title = result.podcast.title,
                artworkUrl = result.podcast.artworkUrl,
                onArtworkClick = result.podcast.artworkUrl?.takeIf(String::isNotBlank)?.let { { showArtworkViewer = true } },
                author = result.podcast.author,
                description = descriptionPreview,
                onDescriptionClick = onDescriptionClick,
                categories = result.podcast.categories,
                onCategory = { categoryName ->
                    onCategory(
                        if (result.providerId == "podcast-index") {
                            com.shapeshed.booth.data.PodcastDiscoveryCategory(
                                id = result.podcast.categoryIds[categoryName] ?: categoryName,
                                title = categoryName,
                                appleGenreId = categoryName,
                            )
                        } else {
                            com.shapeshed.booth.data.PodcastDiscoveryCategories.firstOrNull {
                                it.title.equals(categoryName, ignoreCase = true)
                            }?.let { category -> category.copy(appleGenreId = result.podcast.categoryIds[categoryName] ?: category.appleGenreId) }
                                ?: com.shapeshed.booth.data.PodcastDiscoveryCategory(
                                id = categoryName,
                                title = categoryName,
                                appleGenreId = result.podcast.categoryIds[categoryName] ?: categoryName,
                            )
                        },
                    )
                },
                isSubscribed = isSubscribed,
                onSubscription = if (isSubscribed) onUnsubscribe else onSubscribe,
                showSubscriptionAction = false,
                latestAction = { actionModifier ->
                    val latestEpisode = feed?.episodes?.firstOrNull()
                    val savedLatestEpisode = latestEpisode?.let { savedEpisodes[it.id] }
                    latestEpisode?.let { episode ->
                    PodcastLatestEpisodeButton(
                        modifier = actionModifier,
                        durationMs = episode.durationMs,
                        positionMs = if (episode.id == activeEpisodeId) positionMs else savedLatestEpisode?.positionMs ?: 0L,
                        completed = savedLatestEpisode?.completed == true,
                        isPlaying = episode.id == activeEpisodeId && isPlaying,
                        isBuffering = episode.id == activeEpisodeId && isBuffering,
                        onClick = { onPlay(episode) },
                    )
                    }
                },
                )
            }
        }
        when {
            isLoading -> item(key = "preview-loading") {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
            }
            error != null -> item(key = "preview-error") {
                Text(podcastErrorMessage(error), color = MaterialTheme.colorScheme.error)
            }
            feed != null -> {
                items(feed.episodes, key = { "preview-episode-${it.id}" }) { episode ->
                    val savedEpisode = savedEpisodes[episode.id]
                    val isCompleted = savedEpisode?.completed == true
                    Column(Modifier.fillMaxWidth()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = when {
                                episode.id == activeEpisodeId -> MaterialTheme.colorScheme.surfaceContainerHigh
                                isCompleted -> MaterialTheme.colorScheme.surfaceContainerLow
                                else -> MaterialTheme.colorScheme.surface
                            },
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onEpisodeClick(episode) },
                                    onLongClick = { onLongPress(episode) },
                                ),
                        ) {
                            PodcastActionListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = {
                                    EpisodeTitleBlock(
                                        episode,
                                        episode.id == activeEpisodeId,
                                        downloaded = savedEpisode?.localUri != null || downloadProgress[episode.id]?.completed == true,
                                        downloadProgress = downloadProgress[episode.id],
                                    )
                                },
                                supportingContent = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (
                                            downloadProgress[episode.id]?.completed == true ||
                                            savedEpisode?.localUri != null ||
                                            !episode.audioUrl.isNullOrBlank() ||
                                            !episode.videoUrl.isNullOrBlank() ||
                                            episode.id == activeEpisodeId
                                        ) {
                                            EpisodePlaybackButton(
                                                episode = episode,
                                                positionMs = savedEpisode?.positionMs ?: 0L,
                                                completed = isCompleted,
                                                isPlaying = episode.id == activeEpisodeId && isPlaying,
                                                isBuffering = episode.id == activeEpisodeId && isBuffering,
                                                onClick = { onPlay(episode) },
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { onLongPress(episode) }) {
                                            Icon(Icons.Rounded.MoreHoriz, contentDescription = stringResource(R.string.episode_actions))
                                        }
                                    }
                                },
                                leadingContent = {
                                    PodcastArtwork(
                                        episode.artworkUrl ?: result.podcast.artworkUrl,
                                        episode.title,
                                        Modifier.size(PodcastEpisodeArtworkSize),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    if (showArtworkViewer && !result.podcast.artworkUrl.isNullOrBlank()) {
        PodcastArtworkViewer(
            imageUrl = result.podcast.artworkUrl,
            title = result.podcast.title,
            onDismiss = { showArtworkViewer = false },
        )
    }
}

@Composable
internal fun PodcastPreviewEpisodeScreen(
    episode: com.shapeshed.booth.data.Episode,
    podcastTitle: String,
    podcastArtworkUrl: String?,
    isFavorite: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    completed: Boolean,
    onPlay: () -> Unit,
    onWatch: (() -> Unit)?,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    downloadProgress: DownloadProgress?,
    isInQueue: Boolean,
    onToggleQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenPodcast: () -> Unit,
    isSubscribed: Boolean,
    modifier: Modifier = Modifier,
    twoPane: Boolean = false,
) {
    PodcastEpisodeDetailContent(
        artworkUrl = episode.artworkUrl,
        title = episode.title,
        podcastTitle = podcastTitle,
        podcastArtworkUrl = podcastArtworkUrl,
        descriptionHtml = episode.descriptionHtml,
        audioSizeBytes = episode.audioSizeBytes,
        videoSizeBytes = episode.videoSizeBytes,
        durationMs = episode.durationMs,
        positionMs = positionMs,
        completed = completed,
        publishedAtMillis = episode.publishedAtMillis,
        explicit = episode.explicit,
        isSubscribed = isSubscribed,
        onSubscription = null,
        isPlaying = isPlaying,
        onPlay = onPlay,
        onWatch = onWatch,
        onDownload = onDownload,
        onRemoveDownload = onRemoveDownload,
        downloadProgress = downloadProgress,
        isInQueue = isInQueue,
        onToggleQueue = onToggleQueue,
        isDownloaded = downloadProgress?.completed == true,
        isFavorite = isFavorite,
        isBuffering = isBuffering,
        onToggleFavorite = onToggleFavorite,
        onOpenPodcast = onOpenPodcast,
        twoPane = twoPane,
        modifier = modifier,
    )
}

internal fun episodeRelativeDate(publishedAtMillis: Long): String =
    relativeTime(
        epochSeconds = publishedAtMillis / 1_000L,
        nowSeconds = System.currentTimeMillis() / 1_000L,
    )

internal fun mediaSizeSummary(audioSizeBytes: Long?, videoSizeBytes: Long?): String? =
    listOfNotNull(
        audioSizeBytes?.takeIf { it > 0L }?.let { "Audio ${formatMediaSize(it)}" },
        videoSizeBytes?.takeIf { it > 0L }?.let { "Video ${formatMediaSize(it)}" },
    ).takeIf(List<String>::isNotEmpty)?.joinToString(" · ")

internal fun formatMediaSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (unit == 0) "${bytes} ${units[unit]}"
    else "%.1f %s".format(java.util.Locale.getDefault(), value, units[unit])
}
