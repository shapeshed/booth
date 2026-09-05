package com.shapeshed.booth.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity

@Composable
internal fun PodcastHomeSpecialRootDestination(
    destination: PodcastNavigationKey,
    podcasts: List<PodcastEntity>,
    allPodcastsById: Map<Long, PodcastEntity>,
    downloadAssets: List<DownloadAssetEntity>,
    downloadedEpisodes: Map<Long, EpisodeEntity>,
    playback: PlaybackUiState,
    downloadProgress: Map<Long, DownloadProgress>,
    availableTags: List<String>,
    selectedTags: Set<String>,
    onSelectedTagsChange: (Set<String>) -> Unit,
    viewModel: PodcastViewModel,
    playbackViewModel: PodcastPlaybackViewModel,
    context: Context,
    refreshing: Boolean,
    onOpen: (EpisodeEntity, EpisodeNavigationOrigin) -> Unit,
    onAction: (EpisodeEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        PodcastNavigationKey.Downloads -> PodcastDownloadsContent(
            assets = downloadAssets,
            episodes = downloadedEpisodes,
            podcastsById = allPodcastsById,
            activeEpisodeId = playback.episode?.id,
            isPlaying = playback.isPlaying,
            isBuffering = playback.isBuffering,
            downloadProgress = downloadProgress,
            onOpen = { onOpen(it, EpisodeNavigationOrigin.Downloads) },
            onPlay = { episode -> playbackViewModel.play(episode, allPodcastsById[episode.podcastId]?.title.orEmpty()) },
            onDownload = { viewModel.download(context, it.id) },
            onRemove = { viewModel.removeDownload(context, it.id) },
            onLongPress = onAction,
            modifier = modifier,
        )
        PodcastNavigationKey.AllEpisodes -> {
            val allEpisodePodcastIds = remember(podcasts, selectedTags) {
                podcastIdsForEpisodeTags(podcasts, selectedTags)
            }
            val allEpisodes = remember(allEpisodePodcastIds) {
                viewModel.allEpisodes(allEpisodePodcastIds)
            }.collectAsLazyPagingItems()
            PodcastAllEpisodesContent(
                episodes = allEpisodes,
                podcastsById = allPodcastsById,
                playback = playback,
                availableTags = availableTags,
                selectedTags = selectedTags,
                onSelectedTagsChange = onSelectedTagsChange,
                onOpen = { onOpen(it, EpisodeNavigationOrigin.AllEpisodes) },
                onAddToQueue = { viewModel.addToQueueFromInbox(it.id) },
                onActions = onAction,
                onPlay = { episode -> playbackViewModel.play(episode, allPodcastsById[episode.podcastId]?.title.orEmpty()) },
                onDownload = { viewModel.download(context, it.id) },
                onRefresh = viewModel::refreshSubscriptions,
                refreshing = refreshing,
                downloadProgress = downloadProgress,
                modifier = modifier,
            )
        }
        else -> Unit
    }
}
