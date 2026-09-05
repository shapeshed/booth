package com.shapeshed.booth.ui

import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.data.PodcastEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PodcastHomeSecondaryOverlays(
    routeState: PodcastHomeRouteState,
    homeState: PodcastHomeState,
    selectedPodcast: PodcastEntity?,
    discoveryDescriptionBlocks: List<DescriptionBlock>,
    viewModel: PodcastViewModel,
    undoActions: PodcastHomeUndoActions,
    onUnsubscribeConfirmed: (PodcastEntity) -> Unit,
    directFeedUrl: String,
    onDirectFeedUrlChange: (String) -> Unit,
    onAddPodcast: () -> Unit,
) {
    if (routeState.showAddPodcast.value) {
        AlertDialog(
            onDismissRequest = { routeState.showAddPodcast.value = false },
            title = { Text(stringResource(R.string.add_podcast)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.rss_feed_follow_prompt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = directFeedUrl,
                        onValueChange = onDirectFeedUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.rss_feed_url)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onAddPodcast, enabled = directFeedUrl.isNotBlank()) {
                    Text(stringResource(R.string.add_podcast))
                }
            },
            dismissButton = {
                TextButton(onClick = { routeState.showAddPodcast.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (routeState.showDiscoveryPodcastDescription.value && discoveryDescriptionBlocks.isNotEmpty()) {
        val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
        LaunchedEffect(sheetState) { sheetState.expand() }
        ModalBottomSheet(
            onDismissRequest = { routeState.showDiscoveryPodcastDescription.value = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.about_this_podcast), style = MaterialTheme.typography.headlineSmall)
                discoveryDescriptionBlocks.forEach { block -> DescriptionBlockContent(block) }
            }
        }
    }
    if (routeState.showDiscoverySearch.value) {
        ModalBottomSheet(onDismissRequest = { routeState.showDiscoverySearch.value = false }) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.search_podcasts), style = MaterialTheme.typography.titleLarge)
                SearchPanel(
                    state = homeState,
                    directFeedUrl = "",
                    onDirectFeedUrlChange = {},
                    onSubscribeDirect = {},
                    onQueryChange = viewModel::setQuery,
                    onSearch = {
                        viewModel.search()
                        routeState.showDiscoverySearch.value = false
                    },
                    showDirectFeed = false,
                )
            }
        }
    }
    if (routeState.showUnsubscribeConfirmation.value && selectedPodcast != null) {
        AlertDialog(
            onDismissRequest = { routeState.showUnsubscribeConfirmation.value = false },
            title = { Text(stringResource(R.string.unfollow_podcast_title, selectedPodcast.title)) },
            text = { Text(stringResource(R.string.unfollow_podcast_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        routeState.showUnsubscribeConfirmation.value = false
                        routeState.showPodcastDescription.value = false
                        onUnsubscribeConfirmed(selectedPodcast)
                        undoActions.requestPodcastRemoval(selectedPodcast)
                    },
                ) { Text(stringResource(R.string.unfollow)) }
            },
            dismissButton = {
                TextButton(onClick = { routeState.showUnsubscribeConfirmation.value = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
