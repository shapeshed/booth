package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.shapeshed.booth.data.DownloadProgress
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastEntity

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PodcastAllEpisodesContent(
    episodes: LazyPagingItems<EpisodeEntity>,
    podcastsById: Map<Long, PodcastEntity>,
    availableTags: List<String>,
    selectedTags: Set<String>,
    onSelectedTagsChange: (Set<String>) -> Unit,
    playback: PlaybackUiState,
    onOpen: (EpisodeEntity) -> Unit,
    onAddToQueue: (EpisodeEntity) -> Unit,
    onActions: (EpisodeEntity) -> Unit,
    onPlay: (EpisodeEntity) -> Unit,
    onDownload: (EpisodeEntity) -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean,
    downloadProgress: Map<Long, DownloadProgress>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var showTagSheet by rememberSaveable { mutableStateOf(false) }
    var tagFilterQuery by rememberSaveable { mutableStateOf("") }
    LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 16.dp + LocalPodcastMiniPlayerInset.current,
            ),
            verticalArrangement = Arrangement.spacedBy(PodcastListItemSpacing),
        ) {
            item(key = "all-episodes-filters") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp),
                ) {
                    FilterChip(
                        selected = selectedTags.isNotEmpty(),
                        onClick = { showTagSheet = true },
                        label = {
                            Text(
                                when (selectedTags.size) {
                                    0 -> stringResource(com.shapeshed.booth.R.string.tags)
                                    1 -> selectedTags.first()
                                    else -> "${selectedTags.first()}+${selectedTags.size - 1}"
                                },
                                maxLines = 1,
                            )
                        },
                        leadingIcon = if (selectedTags.isNotEmpty()) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                        trailingIcon = {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                    )
                    if (selectedTags.isNotEmpty()) {
                        TextButton(onClick = { onSelectedTagsChange(emptySet()) }) {
                            Text(stringResource(com.shapeshed.booth.R.string.clear_all))
                        }
                    }
                }
            }
            if (episodes.loadState.refresh is LoadState.Loading && episodes.itemCount == 0) {
                item(key = "all-episodes-loading") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (episodes.loadState.refresh is LoadState.NotLoading && episodes.itemCount == 0) {
                item(key = "all-episodes-empty") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(com.shapeshed.booth.R.string.no_episodes_yet), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(com.shapeshed.booth.R.string.episodes_from_subscriptions),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(
                count = episodes.itemCount,
                key = episodes.itemKey { it.id },
            ) { index ->
                episodes[index]?.let { episode ->
                    InboxEpisodeSwipeRow(
                        episode = episode,
                        podcastTitle = podcastsById[episode.podcastId]?.title.orEmpty(),
                        active = episode.id == playback.episode?.id,
                        selected = false,
                        selectionMode = false,
                        onOpen = { onOpen(episode) },
                        onAddToQueue = { onAddToQueue(episode) },
                        onDismiss = {},
                        onLongPress = {},
                        onActions = { onActions(episode) },
                        onToggleSelection = {},
                        onPlay = { onPlay(episode) },
                        onDownload = { onDownload(episode) },
                        isPlaying = playback.isPlaying,
                        isBuffering = playback.isBuffering,
                        positionOverride = if (episode.id == playback.episode?.id) {
                            playback.durationMs.takeIf { it > 0L }?.let { duration ->
                                (playback.positionMs.toFloat() / duration * (episode.durationMs ?: duration)).toLong()
                            }
                        } else null,
                        downloadProgress = downloadProgress[episode.id],
                        swipeEnabled = false,
                    )
                }
            }
    }
    if (showTagSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showTagSheet = false
                tagFilterQuery = ""
            },
        ) {
            PodcastFilterPickerSheetContent(
                title = stringResource(com.shapeshed.booth.R.string.filter_tags),
                searchLabel = stringResource(com.shapeshed.booth.R.string.search_tags),
                query = tagFilterQuery,
                onQueryChange = { tagFilterQuery = it },
                items = availableTags,
                selectedItems = selectedTags,
                displayName = { it },
                onToggle = { tag ->
                    onSelectedTagsChange(
                        if (tag in selectedTags) selectedTags - tag else selectedTags + tag,
                    )
                },
                itemKey = { it },
                onClear = { onSelectedTagsChange(emptySet()) },
            )
        }
    }
}
