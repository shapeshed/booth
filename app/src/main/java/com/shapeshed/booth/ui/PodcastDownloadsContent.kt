package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity

@Composable
internal fun PodcastDownloadsContent(
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
    PodcastDownloadsScreen(
        assets = assets,
        episodes = episodes,
        podcastsById = podcastsById,
        activeEpisodeId = activeEpisodeId,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        downloadProgress = downloadProgress,
        onOpen = onOpen,
        onPlay = onPlay,
        onDownload = onDownload,
        onRemove = onRemove,
        onLongPress = onLongPress,
        modifier = modifier,
    )
}
