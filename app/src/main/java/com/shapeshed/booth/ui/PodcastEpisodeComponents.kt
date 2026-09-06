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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
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
import androidx.compose.ui.graphics.Shape
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
internal fun EpisodeRow(
    episode: EpisodeEntity,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onPlay: () -> Unit,
    onTogglePlayPause: () -> Unit,
    active: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isBuffering: Boolean = false,
    positionOverride: Long? = null,
    downloaded: Boolean = episode.localUri != null,
    onDownload: () -> Unit,
    downloadProgress: DownloadProgress?,
) {
    Column(modifier.fillMaxWidth()) {
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
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = onLongPress,
                ),
        ) {
            PodcastActionListItem(
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                leadingContent = {
                    PodcastArtwork(
                        episode.artworkUrl,
                        episode.title,
                        Modifier.size(PodcastEpisodeArtworkSize),
                    )
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        EpisodeMetadataLine(episode, downloadProgress, active)
                        EpisodeActionRow(
                            episode = episode,
                            isPlaying = active && isPlaying,
                            isBuffering = active && isBuffering,
                            positionOverride = positionOverride,
                            showPlayback = episode.localUri != null ||
                                downloadProgress?.completed == true ||
                                !episode.audioUrl.isNullOrBlank(),
                            onPlay = if (active) onTogglePlayPause else onPlay,
                            onActions = onLongPress,
                        )
                    }
                },
                headlineContent = {
                        EpisodeTitleBlock(
                            episode,
                            active,
                            downloaded = downloaded,
                            downloadProgress = downloadProgress,
                        )
                },
            )
        }
    }
}

@Composable
internal fun EpisodeTitleBlock(
    episode: EpisodeEntity,
    active: Boolean,
    downloaded: Boolean = episode.localUri != null,
    downloadProgress: DownloadProgress? = null,
    explicit: Boolean? = episode.explicit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        episode.publishedAtMillis?.let { publishedAtMillis ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = episodeDateLabel(publishedAtMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (explicit == true) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ExplicitEpisodeIndicator()
                    }
                }
                Spacer(Modifier.weight(1f))
                if (downloadProgress?.isActive == true && !downloaded) {
                    if (downloadProgress.fraction != null) {
                        CircularProgressIndicator(
                            progress = { downloadProgress.fraction ?: 0f },
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                } else if (downloaded || downloadProgress?.completed == true) {
                    OfflineEpisodeIndicator()
                }
            }
        }
        Text(
            text = episode.title,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                active -> MaterialTheme.colorScheme.onSecondaryContainer
                episode.completed -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun EpisodeTitleBlock(
    episode: com.shapeshed.booth.data.Episode,
    active: Boolean,
    downloaded: Boolean = false,
    downloadProgress: DownloadProgress? = null,
    explicit: Boolean? = episode.explicit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        episode.publishedAtMillis?.let { publishedAtMillis ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = episodeDateLabel(publishedAtMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (explicit == true) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ExplicitEpisodeIndicator()
                    }
                }
                Spacer(Modifier.weight(1f))
                if (downloadProgress?.isActive == true && !downloaded) {
                    if (downloadProgress.fraction != null) {
                        CircularProgressIndicator(
                            progress = { downloadProgress.fraction ?: 0f },
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                } else if (downloaded || downloadProgress?.completed == true) {
                    OfflineEpisodeIndicator()
                }
            }
        }
        Text(
            text = episode.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun EpisodeActionRow(
    episode: EpisodeEntity,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    showPlayback: Boolean,
    positionOverride: Long? = null,
    onPlay: () -> Unit,
    onActions: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showPlayback) {
            EpisodePlaybackButton(
                episode = episode,
                positionOverride = positionOverride,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                onClick = onPlay,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onActions) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = stringResource(R.string.episode_actions))
        }
    }
}

@Composable
internal fun EpisodePlaybackButton(
    episode: EpisodeEntity,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    positionOverride: Long? = null,
    onClick: () -> Unit,
) {
    EpisodePlaybackButton(
        durationMs = episode.durationMs,
        positionMs = positionOverride ?: episode.positionMs,
        completed = episode.completed,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        onClick = onClick,
    )
}

@Composable
internal fun EpisodePlaybackButton(
    episode: com.shapeshed.booth.data.Episode,
    positionMs: Long,
    completed: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    onClick: () -> Unit,
) {
    EpisodePlaybackButton(
        durationMs = episode.durationMs,
        positionMs = positionMs,
        completed = completed,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        onClick = onClick,
    )
}

@Composable
internal fun EpisodePlaybackButton(
    durationMs: Long?,
    positionMs: Long,
    completed: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isBuffering: Boolean = false,
    onClick: () -> Unit,
    prominent: Boolean = false,
    labelOverride: String? = null,
) {
    val label = labelOverride ?: when {
        completed && durationMs != null && durationMs > 0L -> formatPlaybackMinutes(durationMs)
        completed -> stringResource(R.string.play)
        durationMs != null && durationMs > 0L && positionMs > 0L -> formatPlaybackMinutes((durationMs - positionMs).coerceAtLeast(0L))
        durationMs != null && durationMs > 0L -> formatPlaybackMinutes(durationMs)
        else -> stringResource(R.string.play)
    }
    val episodeProgress = durationMs
        ?.takeIf { it > 0L && completed }
        ?.let { 0f }
        ?: durationMs
            ?.takeIf { it > 0L && positionMs > 0L }
            ?.let { (positionMs.toFloat() / it).coerceIn(0f, 1f) }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .height(if (prominent) 48.dp else 36.dp)
            .animateContentSize(animationSpec = tween(220)),
        contentPadding = PaddingValues(
            start = if (prominent) 16.dp else if (isPlaying || isBuffering) 10.dp else 8.dp,
            top = if (prominent) 8.dp else 4.dp,
            end = if (prominent) 16.dp else 12.dp,
            bottom = if (prominent) 8.dp else 4.dp,
        ),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (prominent || isPlaying || isBuffering) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (prominent || isPlaying || isBuffering) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else if (isPlaying) {
                EpisodeEqualizerBars(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(width = 16.dp, height = 14.dp),
                )
            } else {
                Icon(
                    imageVector = if (completed) Icons.Rounded.Replay else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (completed) R.string.replay_episode else R.string.play_episode),
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            episodeProgress?.let { progress ->
                EpisodeInlineProgressBar(
                    progress = progress,
                    active = isPlaying || isBuffering || prominent,
                    modifier = Modifier.width(32.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
internal fun EpisodeInlineProgressBar(
    progress: Float,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val trackColor = if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.24f)
    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.24f)
    val progressColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    Canvas(modifier.height(2.dp)) {
        drawLine(trackColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 2.dp.toPx(), StrokeCap.Butt)
        drawLine(progressColor, Offset(0f, size.height / 2f), Offset(size.width * progress.coerceIn(0f, 1f), size.height / 2f), 2.dp.toPx(), StrokeCap.Butt)
    }
}

internal val EpisodeEqualizerDurations = listOf(380, 440, 310, 500)
internal val EpisodeEqualizerOffsets = listOf(0, 120, 60, 200)

@Composable
internal fun EpisodeEqualizerBars(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "episodeEqualizer")
    val heights = List(4) { index ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(EpisodeEqualizerDurations[index], easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(EpisodeEqualizerOffsets[index]),
            ),
            label = "episodeEqualizerBar$index",
        )
    }
    Canvas(modifier) {
        val gap = size.width * 0.15f / 3f
        val barWidth = (size.width - gap * 3f) / 4f
        heights.forEachIndexed { index, height ->
            val barHeight = size.height * height.value
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

internal fun episodeDateLabel(publishedAtMillis: Long): String {
    val ageMillis = (System.currentTimeMillis() - publishedAtMillis).coerceAtLeast(0L)
    return if (ageMillis >= 7L * 24L * 60L * 60L * 1_000L) {
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
            .format(Instant.ofEpochMilli(publishedAtMillis).atZone(ZoneId.systemDefault()))
    } else {
        episodeRelativeDate(publishedAtMillis)
    }
}

@Composable
internal fun EpisodeMetadataLine(
    episode: EpisodeEntity,
    downloadProgress: DownloadProgress?,
    active: Boolean,
) {
    val metadataColor = if (active) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
    }
}

@Composable
internal fun OfflineEpisodeIndicator() {
    Icon(
        imageVector = Icons.Rounded.OfflinePin,
        contentDescription = stringResource(R.string.available_offline),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
internal fun ExplicitEpisodeIndicator() {
    val explicitContentDescription = stringResource(R.string.explicit_content)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(18.dp)
            .semantics { contentDescription = explicitContentDescription },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "E",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
internal fun EpisodeTrailingSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.widthIn(min = 56.dp).padding(start = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun DownloadEpisodeMetadataLine(
    episode: EpisodeEntity,
    podcastTitle: String,
    asset: DownloadAssetEntity,
    progress: DownloadProgress?,
    active: Boolean,
    downloaded: Boolean,
    completed: Boolean,
) {
    val metadataColor = if (active) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            downloadSizeLabel(asset, episode, progress)?.let { sizeLabel ->
                Text(sizeLabel, style = MaterialTheme.typography.bodySmall, color = metadataColor)
            }
            if (asset.status == DownloadAssetStatus.FAILED || asset.status == DownloadAssetStatus.CANCELLED) {
                DownloadStatusIcon(
                    asset = asset,
                    isDownloaded = downloaded,
                    contentDescription = downloadStatusLabel(asset, downloaded, progress),
                )
            }
        }
    }
}

@Composable
internal fun PodcastEpisodeDetail(
    episode: EpisodeEntity,
    podcastTitle: String,
    podcastArtworkUrl: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlay: () -> Unit,
    onWatch: (() -> Unit)?,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    downloadProgress: DownloadProgress?,
    isInQueue: Boolean,
    onToggleQueue: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenPodcast: () -> Unit,
    isSubscribed: Boolean,
    isSubscriptionLoading: Boolean = false,
    onSubscribe: (() -> Unit)? = null,
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
        positionMs = episode.positionMs,
        completed = episode.completed,
        publishedAtMillis = episode.publishedAtMillis,
        explicit = episode.explicit,
        isSubscribed = isSubscribed,
        isSubscriptionLoading = isSubscriptionLoading,
        onSubscription = onSubscribe,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        onPlay = onPlay,
        onWatch = onWatch,
        onDownload = onDownload,
        onRemoveDownload = onRemoveDownload,
        downloadProgress = downloadProgress,
        isInQueue = isInQueue,
        onToggleQueue = onToggleQueue,
        isDownloaded = episode.localUri != null || downloadProgress?.completed == true,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onOpenPodcast = onOpenPodcast,
        twoPane = twoPane,
        modifier = modifier,
    )
}

@Composable
private fun EpisodeActionButtons(
    durationMs: Long?,
    positionMs: Long,
    completed: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlay: () -> Unit,
    isDownloaded: Boolean,
    isFavorite: Boolean,
    isInQueue: Boolean,
    downloadProgress: DownloadProgress?,
    downloadRequested: Boolean,
    onDownload: () -> Unit,
    onToggleQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteContentDescription = stringResource(
        if (isFavorite) R.string.remove_favorite else R.string.favorite_episode,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EpisodePlaybackButton(
            durationMs = durationMs,
            positionMs = positionMs,
            completed = completed,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            onClick = onPlay,
            prominent = true,
        )
        FilledIconButton(
            onClick = onDownload,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isDownloaded) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isDownloaded) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            when {
                (downloadProgress?.isActive == true || downloadRequested) && !isDownloaded -> {
                    val fraction = downloadProgress?.fraction
                    CircularProgressIndicator(
                        progress = { fraction?.coerceIn(0f, 1f) ?: 0f },
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                }
                isDownloaded -> Icon(Icons.Rounded.OfflinePin, contentDescription = stringResource(R.string.available_offline))
                else -> Icon(Icons.Rounded.FileDownload, contentDescription = stringResource(R.string.download_episode))
            }
        }
        FilledIconButton(
            onClick = onToggleQueue,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isInQueue) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isInQueue) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                imageVector = if (isInQueue) Icons.AutoMirrored.Rounded.PlaylistAddCheck
                else Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = stringResource(
                    if (isInQueue) R.string.remove_from_up_next
                    else R.string.add_to_up_next_accessibility,
                ),
            )
        }
        FilledIconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = favoriteContentDescription
                },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isFavorite) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isFavorite) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
            )
        }
    }
}

@Composable
internal fun PodcastEpisodeDetailContent(
    artworkUrl: String?,
    title: String,
    podcastTitle: String,
    podcastArtworkUrl: String?,
    descriptionHtml: String?,
    audioSizeBytes: Long?,
    videoSizeBytes: Long?,
    durationMs: Long?,
    positionMs: Long,
    completed: Boolean,
    publishedAtMillis: Long?,
    explicit: Boolean?,
    isSubscribed: Boolean,
    isSubscriptionLoading: Boolean = false,
    onSubscription: (() -> Unit)?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isBuffering: Boolean,
    onPlay: () -> Unit,
    onWatch: (() -> Unit)?,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    downloadProgress: DownloadProgress?,
    isInQueue: Boolean,
    onToggleQueue: () -> Unit,
    isDownloaded: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenPodcast: () -> Unit,
    twoPane: Boolean = false,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val descriptionBlocks = remember(descriptionHtml, linkColor) {
        descriptionHtml?.let { formatDescriptionBlocks(it, linkColor) }.orEmpty()
    }
    var showRemoveDownloadConfirmation by rememberSaveable(title) { mutableStateOf(false) }
    var downloadRequested by rememberSaveable(title) { mutableStateOf(false) }
    var showArtworkViewer by rememberSaveable(title) { mutableStateOf(false) }
    LaunchedEffect(isDownloaded) {
        if (isDownloaded) downloadRequested = false
    }
    val detailItems: LazyListScope.() -> Unit = {
        if (!twoPane) {
            item(key = "episode-actions") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    EpisodeActionButtons(
                    durationMs = durationMs,
                    positionMs = positionMs,
                    completed = completed,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    onPlay = onPlay,
                    isDownloaded = isDownloaded,
                    isFavorite = isFavorite,
                    isInQueue = isInQueue,
                    downloadProgress = downloadProgress,
                    downloadRequested = downloadRequested,
                    onDownload = {
                        if (isDownloaded) {
                            showRemoveDownloadConfirmation = true
                        } else if (downloadProgress?.isActive != true) {
                            downloadRequested = true
                            onDownload()
                        }
                    },
                        onToggleQueue = onToggleQueue,
                        onToggleFavorite = onToggleFavorite,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
        }
        item(key = "episode-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                publishedAtMillis?.let { published ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = episodeDateLabel(published),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (explicit == true) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ExplicitEpisodeIndicator()
                        }
                    }
                }
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenPodcast)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PodcastArtwork(
                        imageUrl = podcastArtworkUrl,
                        title = podcastTitle,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        podcastTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSubscribed || onSubscription != null) {
                        PodcastSubscriptionAction(
                            isSubscribed = isSubscribed,
                            isLoading = isSubscriptionLoading,
                            isLastAction = true,
                            onSubscription = onSubscription ?: {},
                            onSubscribe = onSubscription,
                        )
                    }
                }
                mediaSizeSummary(audioSizeBytes, videoSizeBytes)?.let { sizes ->
                    Text(
                        text = sizes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!twoPane) {
                }
                onWatch?.let { watch ->
                    OutlinedButton(onClick = watch, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.VideoLibrary, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.watch))
                    }
                }
                downloadProgress?.takeIf { it.isActive }?.let {
                    Text(
                        text = downloadProgressLabel(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (descriptionBlocks.isNotEmpty()) {
            item(key = "episode-description") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    descriptionBlocks.forEach { block -> DescriptionBlockContent(block) }
                }
            }
        }
    }
    if (twoPane) {
        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .padding(16.dp),
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 48.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    PodcastArtwork(
                        imageUrl = artworkUrl,
                        title = title,
                        modifier = Modifier
                            .requiredSize(maxWidth.coerceAtMost(400.dp))
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomEnd = 16.dp,
                                    bottomStart = 16.dp,
                                ),
                            ),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp,
                        ),
                        onClick = artworkUrl?.takeIf(String::isNotBlank)?.let { { showArtworkViewer = true } },
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    EpisodeActionButtons(
                        durationMs = durationMs,
                        positionMs = positionMs,
                        completed = completed,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        onPlay = onPlay,
                        isDownloaded = isDownloaded,
                        isFavorite = isFavorite,
                        isInQueue = isInQueue,
                        downloadProgress = downloadProgress,
                        downloadRequested = downloadRequested,
                        onDownload = {
                            if (isDownloaded) {
                                showRemoveDownloadConfirmation = true
                            } else if (downloadProgress?.isActive != true) {
                                downloadRequested = true
                                onDownload()
                            }
                        },
                        onToggleQueue = onToggleQueue,
                        onToggleFavorite = onToggleFavorite,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        top = 16.dp,
                        end = 24.dp,
                        bottom = 16.dp + LocalPodcastMiniPlayerInset.current,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = detailItems,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 0.dp,
                end = 16.dp,
                bottom = 16.dp + LocalPodcastMiniPlayerInset.current,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "episode-artwork") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PodcastArtwork(
                        imageUrl = artworkUrl,
                        title = title,
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f),
                        contentScale = ContentScale.Fit,
                        onClick = artworkUrl?.takeIf(String::isNotBlank)?.let { { showArtworkViewer = true } },
                    )
                }
            }
            detailItems()
        }
    }
    if (showArtworkViewer && !artworkUrl.isNullOrBlank()) {
        PodcastArtworkViewer(
            imageUrl = artworkUrl,
            title = title,
            onDismiss = { showArtworkViewer = false },
        )
    }
    if (showRemoveDownloadConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveDownloadConfirmation = false },
            title = { Text(stringResource(R.string.remove_download_question)) },
            text = { Text(stringResource(R.string.remove_download_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDownloadConfirmation = false
                        downloadRequested = false
                        onRemoveDownload()
                    },
                ) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDownloadConfirmation = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
internal fun DownloadProgressIcon(progress: DownloadProgress) {
    val animatedFraction by animateFloatAsState(
        targetValue = progress.fraction?.coerceIn(0f, 1f) ?: 0f,
        label = "download-progress-icon",
    )
    CircularProgressIndicator(
        progress = { animatedFraction },
        modifier = Modifier.padding(12.dp).size(28.dp),
        strokeWidth = 3.dp,
    )
}

@Composable
private fun downloadProgressLabel(progress: DownloadProgress): String {
    val fraction = progress.fraction
    val percent = fraction?.let { stringResource(R.string.download_progress_percent, (it * 100).toInt()) }
        ?: stringResource(R.string.downloading)
    val remaining = when {
        fraction == null -> stringResource(R.string.estimating_time)
        fraction <= 0f || fraction >= 1f -> stringResource(R.string.almost_done)
        else -> {
            val elapsed = (android.os.SystemClock.elapsedRealtime() - progress.startedAtElapsedMs).coerceAtLeast(1L)
            val remainingMs = ((elapsed.toDouble() / fraction) * (1.0 - fraction)).toLong()
            val totalSeconds = (remainingMs / 1_000L).coerceAtLeast(1L)
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            if (minutes > 0L) stringResource(R.string.time_minutes_left, minutes, seconds)
            else stringResource(R.string.time_seconds_left, seconds)
        }
    }
    return "$percent · $remaining"
}

internal val DownloadProgress.isActive: Boolean
    // A queued download has no total size yet, so its fraction is null. It is still
    // active and must render an indeterminate spinner until DownloadManager reports it.
    get() {
        val currentFraction = fraction
        return !completed && (currentFraction == null || currentFraction < 1f)
    }

internal fun formatPlaybackTime(milliseconds: Long): String {
    if (milliseconds <= 0L) return "0:00"
    val totalSeconds = milliseconds / 1_000L
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3_600L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

internal fun formatPlaybackMinutes(milliseconds: Long): String {
    val minutes = ((milliseconds.coerceAtLeast(1L) + 59_999L) / 60_000L)
    if (minutes < 60L) return "$minutes min"
    val hours = minutes / 60L
    val remainingMinutes = minutes % 60L
    return if (remainingMinutes == 0L) {
        "${hours}hr"
    } else {
        "${hours}hr ${remainingMinutes}min"
    }
}

@Composable
internal fun PodcastArtwork(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: (() -> Unit)? = null,
) {
    val artworkClickModifier = onClick?.let { click ->
        Modifier.clickable(onClick = click)
    } ?: Modifier
    Box(
        modifier = modifier
            .clip(shape)
            .then(artworkClickModifier)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(title.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.artwork_description, title),
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}
