package com.shapeshed.booth.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.Episode
import com.shapeshed.booth.data.PodcastDiscoveryCategory

@Composable
internal fun PodcastDiscoveryContent(
    state: PodcastHomeState,
    previewEpisodeEntities: Map<Long, EpisodeEntity>,
    playback: PlaybackUiState,
    viewModel: PodcastViewModel,
    playbackViewModel: PodcastPlaybackViewModel,
    context: Context,
    downloadProgress: Map<Long, DownloadProgress>,
    queueEpisodeIds: List<Long>,
    subscribedFeedUrls: Set<String>,
    onSubscribe: (com.shapeshed.booth.data.PodcastSearchResult) -> Unit,
    onUnsubscribe: () -> Unit,
    onShowDescription: () -> Unit,
    onShowNowPlayingChange: (Boolean) -> Unit,
    onEpisodeAction: (PodcastEpisodeAction) -> Unit,
    onQueueError: () -> Unit,
    onOpenPodcast: () -> Unit,
    onCategory: (PodcastDiscoveryCategory) -> Unit,
    onEpisode: (Episode) -> Unit,
    modifier: Modifier = Modifier,
    twoPane: Boolean = false,
) {
    val previewResult = state.previewResult
    val categoryResult = state.categoryDiscovery
    val previewEpisode = state.previewEpisode
    if (previewEpisode != null && previewResult != null) {
        PodcastPreviewEpisodeScreen(
            episode = previewEpisode,
            podcastTitle = previewResult.podcast.title,
            podcastArtworkUrl = previewResult.podcast.artworkUrl,
            isFavorite = previewEpisodeEntities[previewEpisode.id]?.favorite == true,
            isPlaying = playback.episode?.id == previewEpisode.id && playback.isPlaying,
            isBuffering = playback.episode?.id == previewEpisode.id && playback.isBuffering,
            positionMs = if (playback.episode?.id == previewEpisode.id) playback.positionMs
            else previewEpisodeEntities[previewEpisode.id]?.positionMs ?: 0L,
            completed = previewEpisodeEntities[previewEpisode.id]?.completed == true,
            onPlay = {
                if (playback.episode?.id == previewEpisode.id) playbackViewModel.togglePlayPause()
                else viewModel.preparePreviewEpisode(previewEpisode, previewResult.podcast) { savedEpisode ->
                    playbackViewModel.play(savedEpisode, previewResult.podcast.title)
                }
            },
            onWatch = previewEpisode.videoUrl?.let {
                {
                    viewModel.preparePreviewEpisode(previewEpisode, previewResult.podcast) { savedEpisode ->
                        playbackViewModel.watch(savedEpisode, previewResult.podcast.title)
                        onShowNowPlayingChange(true)
                    }
                }
            },
            onDownload = { viewModel.downloadPreview(context, previewEpisode, previewResult.podcast) },
            onRemoveDownload = {
                viewModel.preparePreviewEpisode(previewEpisode, previewResult.podcast) { savedEpisode ->
                    viewModel.removeDownload(context, savedEpisode.id)
                }
            },
            downloadProgress = downloadProgress[previewEpisode.id],
            isInQueue = previewEpisodeEntities[previewEpisode.id]?.id in queueEpisodeIds,
            onToggleQueue = {
                val savedEpisodeId = previewEpisodeEntities[previewEpisode.id]?.id
                if (savedEpisodeId != null && savedEpisodeId in queueEpisodeIds) {
                    viewModel.removeFromQueue(savedEpisodeId)
                } else {
                    viewModel.preparePreviewEpisode(previewEpisode, previewResult.podcast) { savedEpisode ->
                        viewModel.addToQueueFromInbox(savedEpisode.id, onError = onQueueError)
                    }
                }
            },
            onToggleFavorite = {
                viewModel.preparePreviewEpisode(previewEpisode) { savedEpisode ->
                    viewModel.toggleFavorite(savedEpisode.id)
                }
            },
            onOpenPodcast = onOpenPodcast,
            isSubscribed = previewResult.podcast.feedUrl in subscribedFeedUrls,
            twoPane = twoPane,
            modifier = modifier,
        )
    } else if (categoryResult != null && (state.categoryFromPreview || previewResult == null)) {
        PodcastCategoryListScreen(
            category = categoryResult,
            onBrowse = viewModel::preview,
            onLoadMore = viewModel::loadMoreCategory,
            isLoadingMore = state.isLoadingMoreCategory,
            hasMore = state.hasMoreCategory,
            modifier = modifier,
        )
    } else if (previewResult != null) {
        PodcastDiscoveryPreview(
            result = previewResult,
            feed = state.previewFeed,
            isLoading = state.isLoadingPreview,
            error = state.error,
            onSubscribe = {
                onSubscribe(previewResult)
            },
            isSubscribed = previewResult.podcast.feedUrl in subscribedFeedUrls,
            onDescriptionClick = onShowDescription,
            onUnsubscribe = onUnsubscribe,
            onCategory = onCategory,
            onEpisodeClick = onEpisode,
            activeEpisodeId = playback.episode?.id,
            isPlaying = playback.isPlaying,
            isBuffering = playback.isBuffering,
            positionMs = playback.positionMs,
            playbackProgress = playback.episode?.let {
                playback.durationMs.takeIf { it > 0L }?.let { duration ->
                    (playback.positionMs.toFloat() / duration).coerceIn(0f, 1f)
                }
            },
            onPlay = { episode ->
                if (playback.episode?.id == episode.id) playbackViewModel.togglePlayPause()
                else viewModel.preparePreviewEpisode(episode, previewResult.podcast) { savedEpisode ->
                    playbackViewModel.play(savedEpisode, previewResult.podcast.title)
                }
            },
            onLongPress = { episode ->
                onEpisodeAction(
                    PodcastEpisodeAction.Preview(
                        episode = episode,
                        podcast = previewResult.podcast,
                        podcastTitle = previewResult.podcast.title,
                        podcastArtworkUrl = previewResult.podcast.artworkUrl,
                        linkUrl = episode.linkUrl ?: previewResult.podcast.siteUrl,
                        isDownloaded = previewEpisodeEntities[episode.id]?.localUri != null ||
                            downloadProgress[episode.id]?.completed == true,
                        downloadSizeBytes = episode.audioSizeBytes ?: downloadProgress[episode.id]?.totalBytes,
                        isCompleted = previewEpisodeEntities[episode.id]?.completed == true,
                        isInQueue = episode.id in queueEpisodeIds,
                        hasPlaybackPosition = previewEpisodeEntities[episode.id]?.positionMs?.let { it > 0L } == true,
                    ),
                )
            },
            onDownload = { episode -> viewModel.downloadPreview(context, episode, previewResult.podcast) },
            downloadProgress = downloadProgress,
            savedEpisodes = previewEpisodeEntities,
            modifier = modifier,
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = modifier,
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    }
}
