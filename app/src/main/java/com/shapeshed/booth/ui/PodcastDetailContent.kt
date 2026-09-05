package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.PodcastSearchResult

@Composable
internal fun PodcastDetailContent(
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
    onSubscribe: (PodcastSearchResult) -> Unit,
    onTag: (String) -> Unit,
    onEpisodeClick: (EpisodeEntity) -> Unit,
    onEpisodeLongPress: (EpisodeEntity) -> Unit,
    downloadProgress: Map<Long, DownloadProgress>,
    modifier: Modifier = Modifier,
) {
    PodcastDetail(
        podcast = podcast,
        episodes = episodes,
        onRefresh = onRefresh,
        refreshing = refreshing,
        onPlay = onPlay,
        activeEpisodeId = activeEpisodeId,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        positionMs = positionMs,
        onTogglePlayPause = onTogglePlayPause,
        showDescription = showDescription,
        onShowDescriptionChange = onShowDescriptionChange,
        onDownload = onDownload,
        isSubscribed = isSubscribed,
        onUnsubscribe = onUnsubscribe,
        onSubscribe = onSubscribe,
        onTag = onTag,
        onEpisodeClick = onEpisodeClick,
        onEpisodeLongPress = onEpisodeLongPress,
        downloadProgress = downloadProgress,
        modifier = modifier,
    )
}
