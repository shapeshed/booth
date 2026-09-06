package com.shapeshed.booth.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.shapeshed.booth.PodcastNotificationActionAddToQueue
import com.shapeshed.booth.PodcastNotificationActionPlay

@Composable
internal fun PodcastHomeEffects(
    context: Context,
    routeState: PodcastHomeRouteState,
    viewModel: PodcastViewModel,
    initialEpisodeId: Long?,
    initialNotificationAction: String?,
    playbackViewModel: PodcastPlaybackViewModel,
    savedPodcastTab: String?,
    showNowPlaying: Boolean,
    playbackEpisodeId: Long?,
    showGlobalSearch: Boolean,
    hasActiveDownloads: Boolean,
    selectedEpisode: com.shapeshed.booth.data.EpisodeEntity?,
    onOpenInitialEpisode: (com.shapeshed.booth.data.EpisodeEntity) -> Unit,
    onSelectSavedTab: (PodcastTab) -> Unit,
) {
    LaunchedEffect(Unit) {
        com.shapeshed.booth.data.PodcastRefreshWorker.schedule(
            context = context,
        )
    }
    LaunchedEffect(Unit) { playbackViewModel.connect(context) }
    // Warm the selected catalogue while the home screen is settling. The locale is part of
    // the key so changing the app language cannot leave the previous region's shelf visible.
    LaunchedEffect(context.resources.configuration.locales[0]?.toLanguageTag()) {
        viewModel.loadDiscovery(force = true)
    }
    LaunchedEffect(initialEpisodeId, initialNotificationAction) {
        initialEpisodeId?.let { episodeId ->
            viewModel.episode(episodeId)?.let { episode ->
                onOpenInitialEpisode(episode)
                when (initialNotificationAction) {
                    PodcastNotificationActionAddToQueue -> viewModel.addToQueueFromInbox(episode.id)
                    PodcastNotificationActionPlay -> {
                        val podcastTitle = viewModel.podcast(episode.podcastId)?.title.orEmpty()
                        playbackViewModel.playFromNotification(episode, podcastTitle)
                    }
                }
            }
        }
    }
    LaunchedEffect(savedPodcastTab) {
        savedPodcastTab
            ?.let { value -> runCatching { PodcastTab.valueOf(value) }.getOrNull() }
            ?.let(onSelectSavedTab)
    }
    LaunchedEffect(showNowPlaying, playbackEpisodeId) {
        if (showNowPlaying) playbackViewModel.restoreForegroundVideoPreference()
    }
    LaunchedEffect(showGlobalSearch) {
        if (showGlobalSearch) viewModel.loadDiscovery()
    }
    LaunchedEffect(hasActiveDownloads) {
        viewModel.setDownloadSyncEnabled(context, hasActiveDownloads)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.setDownloadSyncEnabled(context, false) }
    }
    LaunchedEffect(selectedEpisode?.id) {
        selectedEpisode?.let(viewModel::resolveMediaSizes)
    }
}

@Composable
internal fun PodcastHomeBackHandlers(
    routeState: PodcastHomeRouteState,
    playbackViewModel: PodcastPlaybackViewModel,
    inboxSelectionMode: Boolean,
    showGlobalSearch: Boolean,
    onCloseGlobalSearch: () -> Unit,
) {
    BackHandler(enabled = routeState.showNowPlaying.value) {
        playbackViewModel.switchToAudioForBackground()
        routeState.showNowPlaying.value = false
    }
    BackHandler(enabled = routeState.queueReorderMode.value) { routeState.queueReorderMode.value = false }
    BackHandler(enabled = inboxSelectionMode) { routeState.selectedInboxIds.value = emptySet() }
    BackHandler(enabled = showGlobalSearch) { onCloseGlobalSearch() }
    BackHandler(enabled = !routeState.showNowPlaying.value && routeState.showDiscoverySearch.value) {
        routeState.showDiscoverySearch.value = false
    }
}
