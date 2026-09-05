package com.shapeshed.booth.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.QueueEntity
import com.shapeshed.booth.data.PodcastCatalogIndex
import kotlinx.coroutines.flow.emptyFlow

@Immutable
internal data class PodcastHomeDerivedState(
    val selectedPodcast: PodcastEntity?,
    val podcastsById: Map<Long, PodcastEntity>,
    val podcastTitlesById: Map<Long, String>,
    val availablePodcastTags: List<String>,
    val availableAppleCategories: List<String>,
    val queueEpisodeIds: List<Long>,
    val queueEpisodes: List<EpisodeEntity>,
    val downloadedEpisodes: Map<Long, EpisodeEntity>,
    val subscribedFeedUrls: Set<String>,
    val selectedEpisode: EpisodeEntity?,
    val visibleSelectedEpisodes: List<EpisodeEntity>,
    val discoveryDescriptionBlocks: List<DescriptionBlock>,
    val playingPodcastTitle: String?,
)

@Composable
internal fun rememberPodcastHomeDerivedState(
    viewModel: PodcastViewModel,
    podcasts: List<PodcastEntity>,
    catalogIndex: PodcastCatalogIndex,
    queueEntries: List<QueueEntity>,
    queueEpisodes: List<EpisodeEntity>,
    downloadedEpisodes: Map<Long, EpisodeEntity>,
    selectedPodcastId: Long?,
    selectedEpisodeId: Long?,
    homeState: PodcastHomeState,
    playback: PlaybackUiState,
): PodcastHomeDerivedState {
    val queueEpisodeIds = remember(queueEntries) { queueEntries.map { it.episodeId } }
    val selectedEpisodesFlow = remember(viewModel, selectedPodcastId) {
        selectedPodcastId?.let(viewModel::episodes) ?: emptyFlow()
    }
    val selectedEpisodes by selectedEpisodesFlow
        .collectAsStateWithLifecycle(emptyList())
    val visibleSelectedEpisodes = remember(selectedEpisodes, selectedPodcastId) {
        selectedEpisodes.filter { it.podcastId == selectedPodcastId }
    }
    val selectedEpisode = remember(visibleSelectedEpisodes, selectedEpisodeId) {
        visibleSelectedEpisodes.firstOrNull { it.id == selectedEpisodeId }
    }
    val descriptionHtml = homeState.previewFeed?.podcast?.descriptionHtml
        ?: homeState.previewResult?.podcast?.descriptionHtml
    val linkColor = MaterialTheme.colorScheme.primary
    val descriptionBlocks = remember(descriptionHtml, linkColor) {
        descriptionHtml?.let { formatDescriptionBlocks(it, linkColor) }.orEmpty()
    }
    val selectedPodcast = catalogIndex.podcastsById[selectedPodcastId]
    val playingPodcastTitle = remember(playback.episode, catalogIndex.podcastsById) {
        playback.episode?.let { catalogIndex.podcastsById[it.podcastId]?.title }
    }
    return PodcastHomeDerivedState(
        selectedPodcast = selectedPodcast,
        podcastsById = catalogIndex.podcastsById,
        podcastTitlesById = catalogIndex.podcastTitlesById,
        availablePodcastTags = catalogIndex.availableTags,
        availableAppleCategories = catalogIndex.availableAppleCategories,
        queueEpisodeIds = queueEpisodeIds,
        queueEpisodes = queueEpisodes,
        downloadedEpisodes = downloadedEpisodes,
        subscribedFeedUrls = catalogIndex.subscribedFeedUrls,
        selectedEpisode = selectedEpisode,
        visibleSelectedEpisodes = visibleSelectedEpisodes,
        discoveryDescriptionBlocks = descriptionBlocks,
        playingPodcastTitle = playingPodcastTitle,
    )
}
