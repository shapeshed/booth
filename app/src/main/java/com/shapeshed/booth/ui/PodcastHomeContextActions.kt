package com.shapeshed.booth.ui

import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.data.EpisodeEntity

@Composable
internal fun PodcastHomeContextActions(
    showPodcastAppSettings: Boolean,
    showPodcastSettings: Boolean,
    showDiscovery: Boolean,
    hasCategoryDiscovery: Boolean,
    isLocalCategoryDiscovery: Boolean,
    hasPreviewEpisode: Boolean,
    hasPreviewResult: Boolean,
    selectedEpisode: EpisodeEntity?,
    selectedPodcast: PodcastEntity?,
    isPodcastSubscribed: Boolean,
    isPodcastSubscriptionLoading: Boolean,
    isDiscoverySubscribed: Boolean,
    isDiscoverySubscriptionLoading: Boolean,
    podcastMenuExpanded: Boolean,
    context: Context,
    onShowDiscoverySearch: () -> Unit,
    onDiscoverCategory: () -> Unit,
    onPodcastMenuExpandedChange: (Boolean) -> Unit,
    onPodcastSettings: () -> Unit,
    onUnsubscribe: () -> Unit,
    onSubscribe: () -> Unit,
    onDiscoverySubscription: () -> Unit,
    onDiscoverySettings: () -> Unit,
    onDiscoveryShare: () -> Unit,
    onDiscoveryOpenInBrowser: () -> Unit,
) {
    when {
        !showPodcastAppSettings && showDiscovery && hasCategoryDiscovery && isLocalCategoryDiscovery -> {
            androidx.compose.material3.TextButton(onClick = onDiscoverCategory) {
                Text(stringResource(R.string.discover))
            }
        }
        !showPodcastAppSettings && showDiscovery && hasPreviewResult && !hasCategoryDiscovery -> {
            PodcastSubscriptionAction(
                isSubscribed = isDiscoverySubscribed,
                isLoading = isDiscoverySubscriptionLoading,
                isLastAction = true,
                onSubscription = onDiscoverySubscription,
            )
            if (isDiscoverySubscribed) {
                PodcastMenu(
                    expanded = podcastMenuExpanded,
                    onExpandedChange = onPodcastMenuExpandedChange,
                    onSettings = onDiscoverySettings,
                    onUnsubscribe = onDiscoverySubscription,
                    onShare = onDiscoveryShare,
                    onOpenInBrowser = onDiscoveryOpenInBrowser,
                )
            }
        }
        !showPodcastAppSettings && showDiscovery && !hasCategoryDiscovery && !hasPreviewResult -> {
            IconButton(onClick = onShowDiscoverySearch) {
                Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_podcasts))
            }
        }
        !showPodcastAppSettings && !showPodcastSettings && selectedEpisode == null && selectedPodcast != null -> {
            PodcastSubscriptionAction(
                isSubscribed = isPodcastSubscribed,
                isLoading = isPodcastSubscriptionLoading,
                isLastAction = false,
                onSubscription = onUnsubscribe,
                onSubscribe = onSubscribe,
            )
            if (isPodcastSubscribed) {
                PodcastMenu(
                    expanded = podcastMenuExpanded,
                    onExpandedChange = onPodcastMenuExpandedChange,
                    onSettings = onPodcastSettings,
                    onUnsubscribe = onUnsubscribe,
                    onShare = {
                        shareLink(context, selectedPodcast.title, selectedPodcast.siteUrl ?: selectedPodcast.feedUrl)
                    },
                    onOpenInBrowser = {
                        openExternally(context, selectedPodcast.siteUrl ?: selectedPodcast.feedUrl)
                    },
                )
            }
        }
    }
}

@Composable
internal fun PodcastSubscriptionAction(
    isSubscribed: Boolean,
    isLoading: Boolean,
    isLastAction: Boolean,
    onSubscription: () -> Unit,
    onSubscribe: (() -> Unit)? = null,
) {
    if (isLoading) {
        FilledTonalIconButton(
            onClick = {},
            modifier = Modifier.padding(end = if (isLastAction) 12.dp else 0.dp),
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = androidx.compose.ui.Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        }
    } else if (isSubscribed) {
        FilledTonalIconButton(
            onClick = onSubscription,
            modifier = Modifier.padding(end = if (isLastAction) 12.dp else 0.dp),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.unfollow))
        }
    } else {
        FilledTonalButton(
            onClick = onSubscribe ?: onSubscription,
            modifier = Modifier.padding(end = if (isLastAction) 12.dp else 0.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.subscribe))
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.subscribe))
        }
    }
}
