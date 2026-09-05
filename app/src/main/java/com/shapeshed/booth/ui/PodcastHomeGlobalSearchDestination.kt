package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.PodcastEpisodeSearchResult
import com.shapeshed.booth.data.PodcastSearchResult

/** Stateless search destination adapter; navigation and focus events stay at the home boundary. */
@Composable
internal fun PodcastHomeGlobalSearchDestination(
    selectedTab: PodcastTab,
    query: String,
    episodes: List<PodcastEpisodeSearchResult>,
    subscriptions: List<PodcastEntity>,
    remotePodcasts: List<PodcastSearchResult>,
    popularPodcasts: List<PodcastSearchResult>,
    pendingSubscriptions: List<PodcastSearchResult>,
    isLoadingRemote: Boolean,
    error: PodcastUiError?,
    availableTags: List<String>,
    selectedTag: String?,
    onSelectedTagChange: (String?) -> Unit,
    sort: SearchEpisodeSort,
    onSortChange: (SearchEpisodeSort) -> Unit,
    playedFilter: SearchEpisodePlayedFilter,
    onPlayedFilterChange: (SearchEpisodePlayedFilter) -> Unit,
    onOpenEpisode: (com.shapeshed.booth.data.EpisodeEntity) -> Unit,
    onOpenPodcast: (Long) -> Unit,
    onOpenRemotePodcast: (PodcastSearchResult) -> Unit,
    onSubscribe: (PodcastSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    PodcastGlobalSearchContent(
        selectedTab = selectedTab,
        query = query,
        episodes = episodes,
        subscriptions = subscriptions,
        remotePodcasts = remotePodcasts,
        popularPodcasts = popularPodcasts,
        pendingSubscriptions = pendingSubscriptions,
        isLoadingRemote = isLoadingRemote,
        error = error,
        availableTags = availableTags,
        selectedTag = selectedTag,
        onSelectedTagChange = onSelectedTagChange,
        sort = sort,
        onSortChange = onSortChange,
        playedFilter = playedFilter,
        onPlayedFilterChange = onPlayedFilterChange,
        onOpenEpisode = onOpenEpisode,
        onOpenPodcast = onOpenPodcast,
        onOpenRemotePodcast = onOpenRemotePodcast,
        onSubscribe = onSubscribe,
        modifier = modifier,
    )
}
