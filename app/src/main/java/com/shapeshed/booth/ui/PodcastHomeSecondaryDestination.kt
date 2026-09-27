package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.shapeshed.booth.data.DownloadAssetEntity
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import kotlinx.coroutines.flow.StateFlow

/**
 * What the downloads and all-episodes screens can do.
 *
 * This destination routes to two screens, and both were reaching the same ViewModel operations, with
 * the play-with-podcast-title lookup written out in both branches. [play] takes the title as an
 * argument for that reason, so the lookup stays at the one place that knows which podcast a row
 * belongs to.
 */
internal class PodcastSecondaryActions(
    val play: (episode: EpisodeEntity, podcastTitle: String) -> Unit,
    val download: (episodeId: Long) -> Unit,
    val removeDownload: (episodeId: Long) -> Unit,
    val addToQueue: (episodeId: Long) -> Unit,
    val refreshSubscriptions: () -> Unit,
)

@Composable
internal fun PodcastHomeSecondaryDestination(
    destination: PodcastNavigationKey,
    podcasts: List<PodcastEntity>,
    allPodcastsById: Map<Long, PodcastEntity>,
    downloadAssets: List<DownloadAssetEntity>,
    downloadedEpisodes: Map<Long, EpisodeEntity>,
    playback: PlaybackUiState,
    downloadProgress: Map<Long, DownloadProgress>,
    actions: PodcastSecondaryActions,
    allEpisodes: kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<EpisodeEntity>>,
    playbackProgressFlow: StateFlow<PlaybackProgress>,
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
            onPlay = { episode ->
                actions.play(episode, allPodcastsById[episode.podcastId]?.title.orEmpty())
            },
            onDownload = { actions.download(it.id) },
            onRemove = { actions.removeDownload(it.id) },
            onLongPress = onAction,
            modifier = modifier,
        )

        PodcastNavigationKey.AllEpisodes -> {
            val episodes = remember(allEpisodes) { allEpisodes }.collectAsLazyPagingItems()
            // Scoped to this branch: the position ticks twice a second and must not reach the
            // home screen body, and this is the only branch that draws a scrubber.
            val progress by playbackProgressFlow.collectAsStateWithLifecycle()
            PodcastAllEpisodesContent(
                episodes = episodes,
                podcastsById = allPodcastsById,
                playback = playback,
                playbackProgress = progress,
                onOpen = { onOpen(it, EpisodeNavigationOrigin.AllEpisodes) },
                onAddToQueue = { actions.addToQueue(it.id) },
                onActions = onAction,
                onPlay = { episode ->
                    actions.play(episode, allPodcastsById[episode.podcastId]?.title.orEmpty())
                },
                onDownload = { actions.download(it.id) },
                onRefresh = actions.refreshSubscriptions,
                refreshing = refreshing,
                downloadProgress = downloadProgress,
                modifier = modifier,
            )
        }

        else -> Unit
    }
}
