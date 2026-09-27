package com.shapeshed.booth.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.Episode
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.Podcast
import com.shapeshed.booth.data.PodcastDiscoveryCategory
import com.shapeshed.booth.data.PodcastSearchResult
import com.shapeshed.booth.data.isDownloaded

/**
 * What the discovery screen can ask the app to do.
 *
 * This screen used to take both ViewModels and call them inline, which meant it could not be
 * rendered without Hilt and could not be exercised in a test. Each entry is a whole user intent
 * rather than one ViewModel method, because most of them are two steps: a preview episode has to be
 * resolved to a saved row before it can be played, queued, downloaded or favourited. Splitting
 * those into separate callbacks would push the sequencing back into the composable, which is the
 * thing being taken out.
 *
 * [watchPreview] and [addToQueue] take their completion callback as an argument instead of closing
 * over one. That keeps this class free of captured lambdas, so it can be remembered against the
 * ViewModels alone and stay stable while the screen recomposes.
 */
internal class PodcastDiscoveryActions(
    val togglePlayPause: () -> Unit,
    val playPreview: (episode: Episode, podcast: Podcast) -> Unit,
    val watchPreview: (episode: Episode, podcast: Podcast, onWatched: () -> Unit) -> Unit,
    val downloadPreview: (episode: Episode, podcast: Podcast) -> Unit,
    val removeDownload: (episode: Episode, podcast: Podcast) -> Unit,
    val removeFromQueue: (episodeId: Long) -> Unit,
    val addToQueue: (episode: Episode, podcast: Podcast, onError: () -> Unit) -> Unit,
    val toggleFavorite: (episode: Episode) -> Unit,
    val browse: (result: PodcastSearchResult) -> Unit,
    val loadMore: () -> Unit,
)

/**
 * Builds the discovery screen's actions where the ViewModels are in scope.
 *
 * Keyed on the ViewModels alone, which is sound because [PodcastDiscoveryActions] captures no
 * caller lambda. Keying on the callbacks as well would rebuild this object on every recomposition
 * of the home screen, because those lambdas are recreated there each time.
 */
@Composable
internal fun rememberPodcastDiscoveryActions(
    context: Context,
    viewModel: PodcastViewModel,
    playbackViewModel: PodcastPlaybackViewModel,
): PodcastDiscoveryActions = remember(viewModel, playbackViewModel, context) {
    PodcastDiscoveryActions(
        togglePlayPause = playbackViewModel::togglePlayPause,
        playPreview = { episode, podcast ->
            viewModel.preparePreviewEpisode(episode, podcast) { saved ->
                playbackViewModel.play(saved, podcast.title)
            }
        },
        watchPreview = { episode, podcast, onWatched ->
            viewModel.preparePreviewEpisode(episode, podcast) { saved ->
                playbackViewModel.watch(saved, podcast.title)
                onWatched()
            }
        },
        downloadPreview = { episode, podcast ->
            viewModel.downloadPreview(context, episode, podcast)
        },
        removeDownload = { episode, podcast ->
            viewModel.preparePreviewEpisode(episode, podcast) { saved ->
                viewModel.removeDownload(context, saved.id)
            }
        },
        removeFromQueue = { episodeId -> viewModel.removeFromQueue(episodeId) },
        addToQueue = { episode, podcast, onError ->
            viewModel.preparePreviewEpisode(episode, podcast) { saved ->
                viewModel.addToQueueFromInbox(saved.id, onError = onError)
            }
        },
        toggleFavorite = { episode ->
            viewModel.preparePreviewEpisode(episode) { saved ->
                viewModel.toggleFavorite(saved.id)
            }
        },
        browse = { result -> viewModel.preview(result) },
        loadMore = { viewModel.loadMoreCategory() },
    )
}

@Composable
internal fun PodcastDiscoveryContent(
    state: PodcastHomeState,
    previewEpisodeEntities: Map<Long, EpisodeEntity>,
    playback: PlaybackUiState,
    playbackProgress: PlaybackProgress,
    actions: PodcastDiscoveryActions,
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
            positionMs = if (playback.episode?.id == previewEpisode.id) {
                playbackProgress.positionMs
            } else {
                previewEpisodeEntities[previewEpisode.id]?.positionMs ?: 0L
            },
            completed = previewEpisodeEntities[previewEpisode.id]?.completed == true,
            onPlay = {
                if (playback.episode?.id == previewEpisode.id) {
                    actions.togglePlayPause()
                } else {
                    actions.playPreview(previewEpisode, previewResult.podcast)
                }
            },
            onWatch = previewEpisode.videoUrl?.let {
                {
                    actions.watchPreview(previewEpisode, previewResult.podcast) {
                        onShowNowPlayingChange(true)
                    }
                }
            },
            onDownload = { actions.downloadPreview(previewEpisode, previewResult.podcast) },
            onRemoveDownload = { actions.removeDownload(previewEpisode, previewResult.podcast) },
            downloadProgress = downloadProgress[previewEpisode.id],
            isInQueue = previewEpisodeEntities[previewEpisode.id]?.id in queueEpisodeIds,
            onToggleQueue = {
                val savedEpisodeId = previewEpisodeEntities[previewEpisode.id]?.id
                if (savedEpisodeId != null && savedEpisodeId in queueEpisodeIds) {
                    actions.removeFromQueue(savedEpisodeId)
                } else {
                    actions.addToQueue(previewEpisode, previewResult.podcast, onQueueError)
                }
            },
            onToggleFavorite = { actions.toggleFavorite(previewEpisode) },
            onOpenPodcast = onOpenPodcast,
            isSubscribed = previewResult.podcast.feedUrl in subscribedFeedUrls,
            twoPane = twoPane,
            modifier = modifier,
        )
    } else if (categoryResult != null && (state.categoryFromPreview || previewResult == null)) {
        PodcastCategoryListScreen(
            category = categoryResult,
            onBrowse = { result -> actions.browse(result) },
            onLoadMore = { actions.loadMore() },
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
            positionMs = playbackProgress.positionMs,
            playbackProgress = playback.episode?.let { playbackProgress.fraction },
            onPlay = { episode ->
                if (playback.episode?.id == episode.id) {
                    actions.togglePlayPause()
                } else {
                    actions.playPreview(episode, previewResult.podcast)
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
                        isDownloaded = previewEpisodeEntities[episode.id]
                            ?.isDownloaded(downloadProgress[episode.id]) == true,
                        downloadSizeBytes = episode.audioSizeBytes ?: downloadProgress[episode.id]?.totalBytes,
                        isCompleted = previewEpisodeEntities[episode.id]?.completed == true,
                        isInQueue = episode.id in queueEpisodeIds,
                        hasPlaybackPosition = previewEpisodeEntities[episode.id]?.positionMs?.let { it > 0L } == true,
                    ),
                )
            },
            onDownload = { episode -> actions.downloadPreview(episode, previewResult.podcast) },
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
