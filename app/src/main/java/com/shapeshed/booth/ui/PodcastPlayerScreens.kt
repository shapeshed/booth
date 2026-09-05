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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
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
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.material3.adaptive.navigation3.SupportingPaneSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberSupportingPaneSceneStrategy
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
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
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
internal fun PodcastMiniPlayer(
    episode: EpisodeEntity,
    podcastTitle: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(onOpen) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag < -48f) onOpen()
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f },
                )
            }
            .fillMaxWidth(),
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints {
            // Match Aerial's fixed leading/trailing affordances. A weighted middle slot
            // can consume the toolbar's intrinsic width and hide the trailing action.
            val columnWidth = (maxWidth - 140.dp).coerceAtLeast(80.dp)
            val playPauseCorner by animateDpAsState(
                targetValue = if (isPlaying || isBuffering) 50.dp else 14.dp,
                label = "miniPlayerPlayPauseCorner",
            )
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(52.dp),
                    ) {
                        PodcastArtwork(
                            episode.artworkUrl,
                            episode.title,
                            Modifier.fillMaxSize(),
                        )
                    }
                },
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(playPauseCorner),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(playPauseCorner))
                            .clickable(onClick = onPlayPause),
                        ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                        }
                    }
                },
            ) {
                Column(
                    modifier = Modifier
                        .width(columnWidth)
                        .clickable(onClick = onOpen)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        episode.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().safeMarquee(),
                    )
                    Text(
                        podcastTitle ?: stringResource(R.string.now_playing),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().safeMarquee(),
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@SuppressLint("UnsafeOptInUsageError")
internal fun PodcastNowPlayingOverlay(
    episode: EpisodeEntity,
    podcastTitle: String?,
    podcastArtworkUrl: String?,
    onOpenPodcast: (() -> Unit)?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    player: androidx.media3.common.Player?,
    videoMode: Boolean,
    isBuffering: Boolean,
    playbackError: PodcastUiError?,
    speed: Float,
    skipSilence: Boolean,
    sleepTimer: com.shapeshed.booth.data.SleepTimerState?,
    onToggle: () -> Unit,
    onVideoModeChange: (Boolean) -> Unit,
    onSetSleepTimer: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSkipSilenceChange: (Boolean) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    queueEpisodes: List<EpisodeEntity>,
    queuePodcasts: Map<Long, PodcastEntity>,
    queueActiveProgress: Float?,
    onQueuePlay: (EpisodeEntity) -> Unit,
    onQueueRemove: (Long) -> Unit,
    onQueueReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSpeedSheet by rememberSaveable { mutableStateOf(false) }
    var showSleepTimer by rememberSaveable { mutableStateOf(false) }
    var hadActiveSleepTimer by remember { mutableStateOf(sleepTimer != null) }
    var showDescription by rememberSaveable { mutableStateOf(false) }
    var fullScreenVideo by rememberSaveable { mutableStateOf(false) }
    val queueScaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(sleepTimer) {
        if (sleepTimer != null) {
            hadActiveSleepTimer = true
        } else if (hadActiveSleepTimer) {
            showSleepTimer = false
            hadActiveSleepTimer = false
        }
    }

    fun setVideoFullScreen(enabled: Boolean) {
        fullScreenVideo = enabled
    }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    val linkColor = MaterialTheme.colorScheme.primary
    val descriptionBlocks = remember(episode.descriptionHtml, linkColor) {
        episode.descriptionHtml?.let { formatDescriptionBlocks(it, linkColor) }.orEmpty()
    }
    val queueContent: @Composable () -> Unit = {
        PodcastNowPlayingQueueSheet(
            currentEpisode = episode,
            episodes = queueEpisodes,
            podcastsById = queuePodcasts,
            activeEpisodeId = episode.id,
            activeProgress = queueActiveProgress,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            onToggle = onToggle,
            onPlay = onQueuePlay,
            onRemove = onQueueRemove,
            onReorder = onQueueReorder,
            modifier = Modifier.fillMaxSize(),
        )
    }
    val playerContent: @Composable (PaddingValues) -> Unit = { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            if (!fullScreenVideo) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            setVideoFullScreen(false)
                            onDismiss()
                        }) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.close_player))
                        }
                    },
                    actions = {
                        if (descriptionBlocks.isNotEmpty()) {
                            IconButton(onClick = { showDescription = true }) {
                                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.episode_description))
                            }
                        }
                    },
                )
            }
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (videoMode) 0.dp else 24.dp),
            ) {
                val artworkSize = (maxHeight * 0.48f).coerceAtMost(maxWidth)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (videoMode) Arrangement.Top else Arrangement.Center,
                ) {
                    if (videoMode && player != null) {
                        Box(
                            modifier = Modifier
                                .then(
                                    if (fullScreenVideo) Modifier.fillMaxSize()
                                    else Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                                )
                                .clip(if (videoMode) RoundedCornerShape(0.dp) else RoundedCornerShape(28.dp)),
                        ) {
                            AndroidView(
                                factory = { context ->
                                    PlayerView(context).apply {
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        this.player = player
                                    }
                                },
                                update = { it.player = player },
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp).align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp,
                                )
                            }
                            IconButton(
                                onClick = { setVideoFullScreen(!fullScreenVideo) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                                        shape = CircleShape,
                                    ),
                            ) {
                                Icon(
                                    imageVector = if (fullScreenVideo) Icons.Rounded.FullscreenExit
                                    else Icons.Rounded.Fullscreen,
                                    contentDescription = stringResource(if (fullScreenVideo) R.string.exit_full_screen else R.string.full_screen),
                                    tint = Color.White,
                                )
                            }
                        }
                    } else {
                        PodcastArtwork(
                            imageUrl = episode.artworkUrl,
                            title = episode.title,
                            modifier = Modifier.size(artworkSize).clip(RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    if (!fullScreenVideo && episode.videoUrl != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = if (videoMode) 24.dp else 0.dp),
                        ) {
                            FilterChip(
                                selected = !videoMode,
                                onClick = { onVideoModeChange(false) },
                                label = { Text(stringResource(R.string.audio)) },
                            )
                            FilterChip(
                                selected = videoMode,
                                onClick = { onVideoModeChange(true) },
                                label = { Text(stringResource(R.string.video)) },
                            )
                        }
                    }
                    if (!fullScreenVideo) {
                        Spacer(Modifier.height(24.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (videoMode) 24.dp else 0.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    episode.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (episode.explicit == true) {
                                    ExplicitEpisodeIndicator()
                                }
                            }
                            podcastTitle?.let { title ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            onOpenPodcast?.let { Modifier.clickable(onClick = it) }
                                                ?: Modifier,
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (onOpenPodcast != null) {
                                        PodcastArtwork(
                                            imageUrl = podcastArtworkUrl,
                                            title = title,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        val playerProgress = if (durationMs > 0L) {
                            (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        LaunchedEffect(positionMs, durationMs, isScrubbing) {
                            if (!isScrubbing) scrubPosition = playerProgress
                        }
                        Slider(
                            value = if (isScrubbing) scrubPosition else playerProgress,
                            onValueChange = {
                                isScrubbing = true
                                scrubPosition = it
                            },
                            onValueChangeFinished = {
                                if (durationMs > 0L) onSeekTo((scrubPosition * durationMs).toLong())
                                isScrubbing = false
                            },
                            enabled = durationMs > 0L,
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    drawStopIndicator = null,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (videoMode) 24.dp else 0.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (videoMode) 24.dp else 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val displayedPositionMs = if (isScrubbing && durationMs > 0L) {
                                (scrubPosition * durationMs).toLong()
                            } else {
                                positionMs
                            }
                            Text(
                                text = formatPlaybackTime(displayedPositionMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatPlaybackTime(durationMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        playbackError?.let { message ->
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (videoMode) 24.dp else 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = podcastErrorMessage(message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f),
                                )
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry_playback)) }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (videoMode) 24.dp else 0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                PlaybackSpeedAction(speed = speed, onClick = { showSpeedSheet = true })
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FilledTonalIconButton(onClick = onSeekBack) {
                                    Icon(Icons.Rounded.Replay10, contentDescription = stringResource(R.string.back_10_seconds))
                                }
                                FilledIconButton(
                                    onClick = onToggle,
                                    modifier = Modifier.size(72.dp),
                                ) {
                                    if (isBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 3.dp,
                                        )
                                    } else {
                                        Icon(
                                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }
                                }
                                FilledTonalIconButton(onClick = onSeekForward) {
                                    Icon(Icons.Rounded.Forward30, contentDescription = stringResource(R.string.forward_30_seconds))
                                }
                            }
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                SleepTimerAction(active = sleepTimer, onClick = { showSleepTimer = true })
                            }
                        }
                    }
                }
            }
        }
    }
    val isWideWindow = currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(840)
    val nowPlayingBackStack = rememberNavBackStack(PodcastNavigationKey.NowPlaying)
    val supportingPaneStrategy = rememberSupportingPaneSceneStrategy<NavKey>()
    LaunchedEffect(isWideWindow) {
        if (isWideWindow) {
            if (PodcastNavigationKey.NowPlayingQueue !in nowPlayingBackStack) {
                nowPlayingBackStack.add(PodcastNavigationKey.NowPlayingQueue)
            }
        } else {
            nowPlayingBackStack.removeAll { it != PodcastNavigationKey.NowPlaying }
        }
    }
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onDismiss) {
            var totalDrag = 0f
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    if (totalDrag > 48f) {
                        setVideoFullScreen(false)
                        onDismiss()
                    }
                    totalDrag = 0f
                },
                onDragCancel = { totalDrag = 0f },
            )
        },
        color = MaterialTheme.colorScheme.background,
    ) {
        if (isWideWindow) {
            NavDisplay(
                backStack = nowPlayingBackStack,
                onBack = { nowPlayingBackStack.removeLastOrNull() },
                sceneStrategies = listOf(supportingPaneStrategy),
                entryProvider = entryProvider {
                    entry(
                        key = PodcastNavigationKey.NowPlaying,
                        metadata = SupportingPaneSceneStrategy.mainPane(),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            playerContent(PaddingValues(bottom = 16.dp))
                        }
                    }
                    entry(
                        key = PodcastNavigationKey.NowPlayingQueue,
                        metadata = SupportingPaneSceneStrategy.supportingPane(),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            queueContent()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
            )
        } else {
            BottomSheetScaffold(
                scaffoldState = queueScaffoldState,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                sheetPeekHeight = 56.dp,
                sheetSwipeEnabled = true,
                sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                sheetContent = { queueContent() },
            ) { contentPadding ->
                playerContent(contentPadding)
            }
        }
        /*
         * The player pane is shared by the compact bottom-sheet and wide two-pane layouts.
         * Keeping one source of truth prevents tablet controls from drifting from phone controls.
         */
        /* old player content removed */
        /*
        Column(modifier = Modifier.fillMaxSize()) {
            if (!fullScreenVideo) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            setVideoFullScreen(false)
                            onDismiss()
                        }) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.close_player))
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { speedMenuExpanded = true }) {
                                Icon(Icons.Outlined.Speed, contentDescription = stringResource(R.string.playback_speed))
                            }
                            DropdownMenu(
                                expanded = speedMenuExpanded,
                                onDismissRequest = { speedMenuExpanded = false },
                            ) {
                                listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (option == 1f) "1×" else "${option}×",
                                                color = if (speed == option) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = {
                                            onSpeedChange(option)
                                            speedMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        if (descriptionBlocks.isNotEmpty()) {
                            IconButton(onClick = { showDescription = true }) {
                                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.episode_description))
                            }
                        }
                    },
                )
            }
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (videoMode) 0.dp else 24.dp),
            ) {
                val artworkSize = (maxHeight * 0.48f).coerceAtMost(maxWidth)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (videoMode) Arrangement.Top else Arrangement.Center,
                ) {
                    if (videoMode && player != null) {
                        Box(
                            modifier = Modifier
                                .then(
                                    if (fullScreenVideo) Modifier.fillMaxSize()
                                    else Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                                )
                                .clip(if (videoMode) RoundedCornerShape(0.dp) else RoundedCornerShape(28.dp)),
                        ) {
                            AndroidView(
                                factory = { context ->
                                    PlayerView(context).apply {
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        this.player = player
                                    }
                                },
                                update = { it.player = player },
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp).align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp,
                                )
                            }
                            IconButton(
                                onClick = { setVideoFullScreen(!fullScreenVideo) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                                        shape = CircleShape,
                                    ),
                            ) {
                                Icon(
                                    imageVector = if (fullScreenVideo) Icons.Rounded.FullscreenExit
                                    else Icons.Rounded.Fullscreen,
                                    contentDescription = stringResource(if (fullScreenVideo) R.string.exit_full_screen else R.string.full_screen),
                                    tint = Color.White,
                                )
                            }
                        }
                    } else {
                        PodcastArtwork(
                            imageUrl = episode.artworkUrl,
                            title = episode.title,
                            modifier = Modifier.size(artworkSize).clip(RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    if (!fullScreenVideo && episode.videoUrl != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = if (videoMode) 24.dp else 0.dp),
                        ) {
                            FilterChip(
                                selected = !videoMode,
                                onClick = { onVideoModeChange(false) },
                                label = { Text(stringResource(R.string.audio)) },
                            )
                            FilterChip(
                                selected = videoMode,
                                onClick = { onVideoModeChange(true) },
                                label = { Text(stringResource(R.string.video)) },
                            )
                        }
                    }
                    if (!fullScreenVideo) {
                    Spacer(Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (videoMode) 24.dp else 0.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                episode.title,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (episode.explicit == true) {
                                ExplicitEpisodeIndicator()
                            }
                        }
                        podcastTitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    val playerProgress = if (durationMs > 0L) {
                        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    LaunchedEffect(positionMs, durationMs, isScrubbing) {
                        if (!isScrubbing) scrubPosition = playerProgress
                    }
                    Slider(
                        value = if (isScrubbing) scrubPosition else playerProgress,
                        onValueChange = {
                            isScrubbing = true
                            scrubPosition = it
                        },
                        onValueChangeFinished = {
                            if (durationMs > 0L) onSeekTo((scrubPosition * durationMs).toLong())
                            isScrubbing = false
                        },
                        enabled = durationMs > 0L,
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                drawStopIndicator = null,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (videoMode) 24.dp else 0.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (videoMode) 24.dp else 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val displayedPositionMs = if (isScrubbing && durationMs > 0L) {
                            (scrubPosition * durationMs).toLong()
                        } else {
                            positionMs
                        }
                        Text(
                            text = formatPlaybackTime(displayedPositionMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatPlaybackTime(durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    playbackError?.let { message ->
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (videoMode) 24.dp else 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = podcastErrorMessage(message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry_playback)) }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = if (videoMode) 24.dp else 0.dp),
                    ) {
                        FilledTonalIconButton(onClick = onSeekBack) {
                                Icon(Icons.Rounded.Replay10, contentDescription = stringResource(R.string.back_10_seconds))
                        }
                        FilledIconButton(
                            onClick = onToggle,
                            modifier = Modifier.size(72.dp),
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 3.dp,
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        FilledTonalIconButton(onClick = onSeekForward) {
                                Icon(Icons.Rounded.Forward30, contentDescription = stringResource(R.string.forward_30_seconds))
                        }
                    }
                    }
                }
            }
        }
        */
    }
    if (showDescription && descriptionBlocks.isNotEmpty()) {
        val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
        LaunchedEffect(sheetState) { sheetState.expand() }
        ModalBottomSheet(
            onDismissRequest = { showDescription = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.description), style = MaterialTheme.typography.headlineSmall)
                descriptionBlocks.forEach { block ->
                    DescriptionBlockContent(block)
                }
            }
        }
    }
    if (showSpeedSheet) {
        PlaybackSpeedSheet(
            speed = speed,
            skipSilence = skipSilence,
            onSpeedChange = onSpeedChange,
            onSkipSilenceChange = onSkipSilenceChange,
            onDismiss = { showSpeedSheet = false },
        )
    }
    if (showSleepTimer) {
        SleepTimerSheet(
            active = sleepTimer,
            onSet = onSetSleepTimer,
            onCancel = onCancelSleepTimer,
            onDismiss = { showSleepTimer = false },
        )
    }
}

@Composable
internal fun PodcastNowPlayingQueueSheet(
    currentEpisode: EpisodeEntity,
    episodes: List<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    activeEpisodeId: Long,
    activeProgress: Float?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onToggle: () -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onRemove: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val upcomingEpisodes = remember(episodes, activeEpisodeId) {
        episodes.filterNot { it.id == activeEpisodeId }
    }
    var orderedEpisodes by remember(upcomingEpisodes) { mutableStateOf(upcomingEpisodes) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(upcomingEpisodes) {
        if (draggingId == null) orderedEpisodes = upcomingEpisodes
    }

    Column(
        modifier = modifier
            .fillMaxHeight(),
    ) {
        ListItem(
                modifier = Modifier.padding(horizontal = 8.dp),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    PodcastArtwork(
                        currentEpisode.artworkUrl,
                        currentEpisode.title,
                        Modifier.size(PodcastEpisodeArtworkSize),
                    )
                },
                supportingContent = {
                    Text(
                        podcastsById[currentEpisode.podcastId]?.title.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    FilledTonalIconButton(
                        onClick = {
                            if (currentEpisode.id == activeEpisodeId) onToggle()
                            else onPlay(currentEpisode)
                        },
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                            )
                        }
                    }
                },
        ) {
            Text(
                currentEpisode.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        if (orderedEpisodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        stringResource(R.string.nothing_queued),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.nothing_queued_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = listState,
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(PodcastListItemSpacing),
            ) {
                items(orderedEpisodes, key = { "now-queue-${it.id}" }) { episode ->
                    val isDragging = draggingId == episode.id
                    QueueEpisodeSwipeRow(
                        episode = episode,
                        podcastTitle = podcastsById[episode.podcastId]?.title.orEmpty(),
                        active = episode.id == activeEpisodeId,
                        dragging = isDragging,
                        progressOverride = episode.id.takeIf { it == activeEpisodeId }?.let { activeProgress },
                        positionOverride = episode.id.takeIf { it == activeEpisodeId }?.let { id ->
                            activeProgress?.let { fraction ->
                                orderedEpisodes.firstOrNull { it.id == id }?.durationMs
                                    ?.takeIf { it > 0L }
                                    ?.times(fraction)
                                    ?.toLong()
                            }
                        },
                        isPlaying = isPlaying,
                        isBuffering = episode.id == activeEpisodeId && isBuffering,
                        downloadProgress = null,
                        onOpen = { onPlay(episode) },
                        onPlay = { onPlay(episode) },
                        onDownload = {},
                        onLongPress = {},
                        onRemove = {
                            onRemove(episode.id)
                            Result.success(Unit)
                        },
                        showDragHandle = true,
                        compact = true,
                        modifier = Modifier
                            .animateItem()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragging) dragDistance else 0f },
                        dragHandleModifier = Modifier.pointerInput(episode.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = episode.id
                                    dragDistance = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDistance += dragAmount.y
                                    val currentIndex = orderedEpisodes.indexOfFirst { it.id == episode.id }
                                    val currentInfo = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == "now-queue-${episode.id}" }
                                        ?: return@detectDragGesturesAfterLongPress
                                    val center = currentInfo.offset + currentInfo.size / 2 + dragDistance
                                    val targetInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                        info.key.toString().startsWith("now-queue-") &&
                                            info.key != currentInfo.key &&
                                            center > info.offset && center < info.offset + info.size
                                    } ?: return@detectDragGesturesAfterLongPress
                                    val targetId = targetInfo.key.toString().removePrefix("now-queue-").toLongOrNull()
                                        ?: return@detectDragGesturesAfterLongPress
                                    val targetIndex = orderedEpisodes.indexOfFirst { it.id == targetId }
                                    if (targetIndex >= 0 && targetIndex != currentIndex) {
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
                        },
                    )
                }
            }
        }
    }
}

internal sealed interface DescriptionBlock {
    data class TextBlock(val text: AnnotatedString, val role: Role) : DescriptionBlock
    data class ListBlock(val items: List<AnnotatedString>, val ordered: Boolean) : DescriptionBlock
    data class ImageBlock(val url: String, val description: String?) : DescriptionBlock

    enum class Role { Body, Heading, Quote }
}

@Composable
internal fun DescriptionBlockContent(block: DescriptionBlock) {
    when (block) {
        is DescriptionBlock.TextBlock -> ClickableDescriptionText(
            text = block.text,
            style = when (block.role) {
                DescriptionBlock.Role.Body -> MaterialTheme.typography.bodyLarge
                DescriptionBlock.Role.Heading -> MaterialTheme.typography.titleLarge
                DescriptionBlock.Role.Quote -> MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
            },
            modifier = if (block.role == DescriptionBlock.Role.Quote) {
                Modifier.padding(start = 16.dp)
            } else {
                Modifier
            },
        )
        is DescriptionBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            block.items.forEachIndexed { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (block.ordered) "${index + 1}." else "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ClickableDescriptionText(item, modifier = Modifier.weight(1f))
                }
            }
        }
        is DescriptionBlock.ImageBlock -> AsyncImage(
            model = block.url,
            contentDescription = block.description,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(192.dp)
                .clip(MaterialTheme.shapes.medium),
        )
    }
}

@Composable
private fun ClickableDescriptionText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val uriHandler = LocalUriHandler.current
    var layoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        modifier = modifier.pointerInput(text) {
            detectTapGestures { position: Offset ->
                val offset = layoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
                text.getStringAnnotations(DescriptionUrlAnnotation, offset, offset)
                    .firstOrNull()
                    ?.item
                    ?.let(uriHandler::openUri)
            }
        },
        onTextLayout = { layoutResult = it },
    )
}

internal fun formatDescriptionBlocks(html: String, linkColor: Color): List<DescriptionBlock> {
    val body = Jsoup.parse(html).body()
    val blocks = flattenDescriptionElements(body)
    return blocks.mapNotNull { block ->
        when (block.tagName()) {
            "img" -> block.attr("abs:src").ifBlank { block.attr("src") }
                .takeIf(String::isNotBlank)
                ?.let { DescriptionBlock.ImageBlock(it, block.attr("alt").ifBlank { null }) }
            "ul", "ol" -> DescriptionBlock.ListBlock(
                items = block.children().filter { it.tagName() == "li" }.mapNotNull { item ->
                    buildAnnotatedString { appendDescriptionNode(item, linkColor) }
                        .takeIf { it.text.isNotBlank() }
                },
                ordered = block.tagName() == "ol",
            )
            else -> {
                val role = when (block.tagName()) {
                    "h1", "h2", "h3", "h4", "h5", "h6" -> DescriptionBlock.Role.Heading
                    "blockquote" -> DescriptionBlock.Role.Quote
                    else -> DescriptionBlock.Role.Body
                }
                buildAnnotatedString { appendDescriptionNode(block, linkColor) }
                    .takeIf { it.text.isNotBlank() }
                    ?.let { DescriptionBlock.TextBlock(it, role) }
            }
        }
    }
}

private val descriptionBlockTags = setOf(
    "p", "div", "section", "article", "header", "footer",
    "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "ul", "ol", "img",
)

private fun flattenDescriptionElements(element: Element): List<Element> {
    val children = element.children()
    if (children.isEmpty()) return listOf(element)

    val containsNestedBlocks = children.any { it.tagName() in descriptionBlockTags }
    if (!containsNestedBlocks) return listOf(element)

    return children.flatMap { child ->
        if (child.tagName() in setOf("div", "section", "article") &&
            child.children().any { it.tagName() in descriptionBlockTags }
        ) {
            flattenDescriptionElements(child)
        } else {
            listOf(child)
        }
    }
}

private fun AnnotatedString.Builder.appendDescriptionNode(node: Node, linkColor: Color) {
    when (node) {
        is TextNode -> {
            val text = node.getWholeText()
                .replace("\r\n", "\n")
                .replace(Regex("[\\t\\u000B\\f\\r ]+"), " ")
                .replace(Regex(" *\\n *\\n+"), "\n\n")
                .replace(Regex(" *\\n *"), "\n")
            appendTextWithLinks(text, linkColor)
        }
        is Element -> {
            if (node.tagName() == "br") {
                append("\n")
            } else if (node.tagName() == "a" && node.hasAttr("href")) {
                pushStringAnnotation(DescriptionUrlAnnotation, node.attr("abs:href").ifBlank { node.attr("href") })
                withStyle(SpanStyle(color = linkColor, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                    append(node.text())
                }
                pop()
            } else if (node.tagName() == "strong" || node.tagName() == "b") {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    node.childNodes().forEach { appendDescriptionNode(it, linkColor) }
                }
            } else if (node.tagName() == "em" || node.tagName() == "i") {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    node.childNodes().forEach { appendDescriptionNode(it, linkColor) }
                }
            } else if (node.tagName() == "u") {
                withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                    node.childNodes().forEach { appendDescriptionNode(it, linkColor) }
                }
            } else {
                node.childNodes().forEach { appendDescriptionNode(it, linkColor) }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendTextWithLinks(text: String, linkColor: Color) {
    var cursor = 0
    PlainUrlPattern.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val url = match.value.trimEnd('.', ',', ')', ']', ';')
        pushStringAnnotation(DescriptionUrlAnnotation, url)
        withStyle(
            SpanStyle(
                color = linkColor,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
            ),
        ) {
            append(url)
        }
        pop()
        append(match.value.removePrefix(url))
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

private val PlainUrlPattern = Regex("https?://[^\\s<>\\\"']+")

private const val DescriptionUrlAnnotation = "description-url"
