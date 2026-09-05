package com.shapeshed.booth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

@Stable
internal class PodcastHomeRouteState internal constructor(
    val showSearch: MutableState<Boolean>,
    val selectedPodcastId: MutableState<Long?>,
    val podcastDetailFromSubscriptions: MutableState<Boolean>,
    val podcastDetailPageId: MutableState<Long?>,
    val selectedEpisodeId: MutableState<Long?>,
    val episodeOrigin: MutableState<EpisodeOrigin>,
    val showNowPlaying: MutableState<Boolean>,
    val podcastSortOrder: MutableState<PodcastSortOrder>,
    val showPodcastDescription: MutableState<Boolean>,
    val showUnsubscribeConfirmation: MutableState<Boolean>,
    val podcastMenuExpanded: MutableState<Boolean>,
    val selectedTab: MutableState<PodcastTab>,
    val showDiscoverySearch: MutableState<Boolean>,
    val showDiscoveryPodcastDescription: MutableState<Boolean>,
    val rootMenuExpanded: MutableState<Boolean>,
    val allEpisodeTags: MutableState<Set<String>>,
    val inboxSelectionMenuExpanded: MutableState<Boolean>,
    val selectedInboxIds: MutableState<Set<Long>>,
    val queueFilter: MutableState<QueueFilter>,
    val pendingEpisodeAction: MutableState<PodcastEpisodeAction?>,
    val queueReorderMode: MutableState<Boolean>,
    val directFeedUrl: MutableState<String>,
    val showAddPodcast: MutableState<Boolean>,
)

@Composable
internal fun rememberPodcastHomeRouteState(): PodcastHomeRouteState = PodcastHomeRouteState(
    showSearch = rememberSaveable { mutableStateOf(false) },
    selectedPodcastId = rememberSaveable { mutableStateOf<Long?>(null) },
    podcastDetailFromSubscriptions = rememberSaveable { mutableStateOf(false) },
    podcastDetailPageId = rememberSaveable { mutableStateOf<Long?>(null) },
    selectedEpisodeId = rememberSaveable { mutableStateOf<Long?>(null) },
    episodeOrigin = rememberSaveable { mutableStateOf(EpisodeOrigin.INBOX) },
    showNowPlaying = rememberSaveable { mutableStateOf(false) },
    podcastSortOrder = rememberSaveable { mutableStateOf(PodcastSortOrder.LAST_UPDATED) },
    showPodcastDescription = rememberSaveable { mutableStateOf(false) },
    showUnsubscribeConfirmation = rememberSaveable { mutableStateOf(false) },
    podcastMenuExpanded = rememberSaveable { mutableStateOf(false) },
    selectedTab = rememberSaveable { mutableStateOf(PodcastTab.SUBSCRIPTIONS) },
    showDiscoverySearch = rememberSaveable { mutableStateOf(false) },
    showDiscoveryPodcastDescription = rememberSaveable { mutableStateOf(false) },
    rootMenuExpanded = rememberSaveable { mutableStateOf(false) },
    allEpisodeTags = remember { mutableStateOf(emptySet()) },
    inboxSelectionMenuExpanded = rememberSaveable { mutableStateOf(false) },
    selectedInboxIds = remember { mutableStateOf(emptySet()) },
    queueFilter = rememberSaveable { mutableStateOf(QueueFilter.ALL) },
    pendingEpisodeAction = remember { mutableStateOf<PodcastEpisodeAction?>(null) },
    queueReorderMode = remember { mutableStateOf(false) },
    directFeedUrl = rememberSaveable { mutableStateOf("") },
    showAddPodcast = rememberSaveable { mutableStateOf(false) },
)
