package com.shapeshed.booth.ui

import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R
import com.shapeshed.booth.data.searchableCategories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.PodcastEpisodeSearchResult
import com.shapeshed.booth.data.PodcastSearchResult

internal enum class SearchEpisodeSort { RELEVANCE, NEWEST, OLDEST, TITLE }
internal enum class SearchEpisodePlayedFilter { ALL, UNPLAYED, IN_PROGRESS, PLAYED }

@Composable
internal fun PodcastGlobalSearchContent(
    selectedTab: PodcastTab,
    query: String,
    episodes: List<PodcastEpisodeSearchResult>,
    subscriptions: List<PodcastEntity>,
    remotePodcasts: List<PodcastSearchResult>,
    modifier: Modifier = Modifier,
    popularPodcasts: List<PodcastSearchResult> = emptyList(),
    pendingSubscriptions: List<PodcastSearchResult> = emptyList(),
    isLoadingRemote: Boolean,
    error: PodcastUiError?,
    onOpenEpisode: (EpisodeEntity) -> Unit,
    onOpenPodcast: (Long) -> Unit,
    onOpenRemotePodcast: (PodcastSearchResult) -> Unit = {},
    onSubscribe: (PodcastSearchResult) -> Unit,
    availableTags: List<String> = emptyList(),
    selectedTag: String? = null,
    onSelectedTagChange: (String?) -> Unit = {},
    sort: SearchEpisodeSort = SearchEpisodeSort.NEWEST,
    onSortChange: (SearchEpisodeSort) -> Unit = {},
    playedFilter: SearchEpisodePlayedFilter = SearchEpisodePlayedFilter.ALL,
    onPlayedFilterChange: (SearchEpisodePlayedFilter) -> Unit = {},
) {
    val normalizedQuery = query.trim()
    val localPodcasts = subscriptions.filter { podcast ->
        normalizedQuery.isNotEmpty() && (
            podcast.title.contains(normalizedQuery, ignoreCase = true) ||
                podcast.author.orEmpty().contains(normalizedQuery, ignoreCase = true)
        )
    }
    val subscribedIds = subscriptions.map(PodcastEntity::id).toSet()
    val subscribedFeedUrls = subscriptions.map { it.feedUrl }.toSet()
    val pendingPodcasts = pendingSubscriptions
        .filterNot { it.podcast.feedUrl in subscribedFeedUrls }
        .filter { result ->
            normalizedQuery.isNotEmpty() && (
                result.podcast.title.contains(normalizedQuery, ignoreCase = true) ||
                    result.podcast.author.orEmpty().contains(normalizedQuery, ignoreCase = true)
            )
        }
        .distinctBy { it.podcast.feedUrl }
    val discoverablePodcasts = (if (query.isBlank()) popularPodcasts else remotePodcasts)
        .distinctBy { it.podcast.feedUrl }
        .filterNot {
            it.podcast.id in subscribedIds || it.podcast.feedUrl in subscribedFeedUrls
        }
    val filteredEpisodes = episodes
        .filter { result ->
            val episode = result.episode
            when (playedFilter) {
                SearchEpisodePlayedFilter.ALL -> true
                SearchEpisodePlayedFilter.UNPLAYED -> !episode.completed && episode.positionMs <= 0L
                SearchEpisodePlayedFilter.IN_PROGRESS -> !episode.completed && episode.positionMs > 0L
                SearchEpisodePlayedFilter.PLAYED -> episode.completed
            }
        }
        .filter { result ->
            selectedTag == null || subscriptions
                .firstOrNull { it.id == result.episode.podcastId }
                ?.searchableCategories()
                ?.any { it.equals(selectedTag, ignoreCase = true) } == true
        }
        .let { results ->
            when (sort) {
                SearchEpisodeSort.RELEVANCE -> results
                SearchEpisodeSort.NEWEST -> results.sortedByDescending { it.episode.publishedAtMillis ?: Long.MIN_VALUE }
                SearchEpisodeSort.OLDEST -> results.sortedBy { it.episode.publishedAtMillis ?: Long.MAX_VALUE }
                SearchEpisodeSort.TITLE -> results.sortedBy { it.episode.title.lowercase() }
            }
        }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (selectedTab == PodcastTab.SUBSCRIPTIONS) {
            if (localPodcasts.isNotEmpty() || pendingPodcasts.isNotEmpty()) {
                item(key = "your-subscriptions") {
                    Text(
                        stringResource(R.string.your_subscriptions),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                items(localPodcasts, key = { "subscription-${it.id}" }) { podcast ->
                    PodcastActionListItem(
                        leadingContent = { PodcastArtwork(podcast.artworkUrl, podcast.title, Modifier.size(64.dp)) },
                        supportingContent = {
                            podcast.author?.takeIf(String::isNotBlank)?.let { author ->
                                Text(author, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        trailingContent = { SubscribedPodcastIndicator() },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPodcast(podcast.id) },
                    ) {
                        Text(
                            podcast.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                items(pendingPodcasts, key = { "pending-subscription-${it.podcast.feedUrl}" }) { result ->
                    PodcastActionListItem(
                        leadingContent = {
                            PodcastArtwork(result.podcast.artworkUrl, result.podcast.title, Modifier.size(64.dp))
                        },
                        supportingContent = {
                            result.podcast.author?.takeIf(String::isNotBlank)?.let { author ->
                                Text(author, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        trailingContent = { PendingSubscriptionIndicator() },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRemotePodcast(result) },
                    ) {
                        Text(
                            result.podcast.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            if (discoverablePodcasts.isNotEmpty()) {
                item(key = "discover-podcasts") {
                    Text(
                        if (query.isBlank()) stringResource(R.string.popular_podcasts) else stringResource(R.string.discover_podcasts_results),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                // Feed URLs are the stable identity at this boundary. Catalogue IDs can be
                // duplicated or recycled by providers (especially Apple search results), which
                // would otherwise crash LazyColumn with duplicate keys.
                items(discoverablePodcasts, key = { "remote-${it.podcast.feedUrl}" }) { result ->
                    PodcastActionListItem(
                        leadingContent = {
                            PodcastArtwork(result.podcast.artworkUrl, result.podcast.title, Modifier.size(64.dp))
                        },
                        supportingContent = {
                            result.podcast.author?.takeIf(String::isNotBlank)?.let { author ->
                                Text(author, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        trailingContent = {
                            FilledTonalIconButton(onClick = { onSubscribe(result) }) {
                                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.subscribe))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRemotePodcast(result) },
                    ) {
                        Text(
                            result.podcast.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            if (isLoadingRemote) item(key = "remote-loading") {
                Text(stringResource(R.string.searching_podcasts), modifier = Modifier.padding(16.dp))
            }
            if (error != null) item(key = "remote-error") { Text(podcastErrorMessage(error), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        } else {
            item(key = "episode-search-filters") {
                EpisodeSearchFilters(
                    availableTags = availableTags,
                    selectedTag = selectedTag,
                    onSelectedTagChange = onSelectedTagChange,
                    sort = sort,
                    onSortChange = onSortChange,
                    playedFilter = playedFilter,
                    onPlayedFilterChange = onPlayedFilterChange,
                )
            }
            if (filteredEpisodes.isNotEmpty()) {
                items(filteredEpisodes, key = { "episode-${it.episode.id}" }) { result ->
                    SearchEpisodeRow(
                        result = result,
                        onOpen = { onOpenEpisode(result.episode) },
                    )
                }
            }
        }
        if (query.length >= 2 && filteredEpisodes.isEmpty() && localPodcasts.isEmpty() && discoverablePodcasts.isEmpty() && !isLoadingRemote) {
            item(key = "no-results") {
                Text(stringResource(R.string.no_results), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PendingSubscriptionIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        strokeWidth = 2.dp,
    )
}

@Composable
private fun SubscribedPodcastIndicator() {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.subscribed),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun EpisodeSearchFilters(
    availableTags: List<String>,
    selectedTag: String?,
    onSelectedTagChange: (String?) -> Unit,
    sort: SearchEpisodeSort,
    onSortChange: (SearchEpisodeSort) -> Unit,
    playedFilter: SearchEpisodePlayedFilter,
    onPlayedFilterChange: (SearchEpisodePlayedFilter) -> Unit,
) {
    var sortExpanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var playedExpanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var tagExpanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            FilterChip(
                selected = sort != SearchEpisodeSort.RELEVANCE,
                onClick = { sortExpanded.value = true },
                label = { Text(sort.localizedLabel()) },
            )
            DropdownMenu(expanded = sortExpanded.value, onDismissRequest = { sortExpanded.value = false }) {
                SearchEpisodeSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.localizedLabel()) },
                        onClick = { onSortChange(option); sortExpanded.value = false },
                    )
                }
            }
        }
        Box {
            FilterChip(
                selected = playedFilter != SearchEpisodePlayedFilter.ALL,
                onClick = { playedExpanded.value = true },
                label = { Text(playedFilter.localizedLabel()) },
            )
            DropdownMenu(expanded = playedExpanded.value, onDismissRequest = { playedExpanded.value = false }) {
                SearchEpisodePlayedFilter.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.localizedLabel()) },
                        onClick = { onPlayedFilterChange(option); playedExpanded.value = false },
                    )
                }
            }
        }
        if (availableTags.isNotEmpty()) {
            Box {
                FilterChip(
                    selected = selectedTag != null,
                    onClick = { tagExpanded.value = true },
                    label = { Text(selectedTag ?: "Tag") },
                )
                DropdownMenu(expanded = tagExpanded.value, onDismissRequest = { tagExpanded.value = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.all_tags)) },
                        onClick = { onSelectedTagChange(null); tagExpanded.value = false },
                    )
                    availableTags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag) },
                            onClick = { onSelectedTagChange(tag); tagExpanded.value = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEpisodeSort.localizedLabel(): String = when (this) {
    SearchEpisodeSort.RELEVANCE -> stringResource(R.string.relevance)
    SearchEpisodeSort.NEWEST -> stringResource(R.string.newest)
    SearchEpisodeSort.OLDEST -> stringResource(R.string.oldest)
    SearchEpisodeSort.TITLE -> stringResource(R.string.title)
}

@Composable
private fun SearchEpisodePlayedFilter.localizedLabel(): String = when (this) {
    SearchEpisodePlayedFilter.ALL -> stringResource(R.string.played_all)
    SearchEpisodePlayedFilter.UNPLAYED -> stringResource(R.string.search_unplayed)
    SearchEpisodePlayedFilter.IN_PROGRESS -> stringResource(R.string.search_in_progress)
    SearchEpisodePlayedFilter.PLAYED -> stringResource(R.string.played)
}

@Composable
private fun SearchEpisodeRow(
    result: PodcastEpisodeSearchResult,
    onOpen: () -> Unit,
) {
    val episode = result.episode
    PodcastActionListItem(
        leadingContent = {
            PodcastArtwork(
                episode.artworkUrl ?: result.podcastArtworkUrl,
                episode.title,
                Modifier.size(64.dp),
            )
        },
        supportingContent = {
            Column {
                Text(result.podcastTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                result.podcastAuthor?.takeIf(String::isNotBlank)?.let { author ->
                    Text(author, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
