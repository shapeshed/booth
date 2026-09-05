package com.shapeshed.booth.ui

import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity

internal fun selectPodcastTab(
    routeState: PodcastHomeRouteState,
    viewModel: PodcastViewModel,
    tab: PodcastTab,
) {
    routeState.selectedTab.value = tab
    routeState.queueReorderMode.value = false
    viewModel.setPodcastSelectedTab(tab.name)
    routeState.selectedPodcastId.value = null
    routeState.selectedEpisodeId.value = null
    routeState.showDiscoverySearch.value = false
    routeState.showPodcastDescription.value = false
    routeState.selectedInboxIds.value = emptySet()
}

internal fun createSubscribedEpisodeAction(
    episode: EpisodeEntity,
    podcastsById: Map<Long, PodcastEntity>,
    downloadProgress: Map<Long, DownloadProgress>,
    queueEpisodeIds: List<Long>,
): PodcastEpisodeAction.Subscribed = PodcastEpisodeAction.Subscribed(
    episode = episode,
    podcastTitle = podcastsById[episode.podcastId]?.title.orEmpty(),
    podcastArtworkUrl = podcastsById[episode.podcastId]?.artworkUrl,
    linkUrl = episode.linkUrl ?: podcastsById[episode.podcastId]?.siteUrl,
    isDownloaded = episode.localUri != null || downloadProgress[episode.id]?.completed == true,
    downloadSizeBytes = episode.audioSizeBytes ?: downloadProgress[episode.id]?.totalBytes,
    isCompleted = episode.completed,
    isInQueue = queueEpisodeIds.contains(episode.id),
    hasPlaybackPosition = episode.positionMs > 0L,
)
