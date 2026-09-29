package com.shapeshed.booth.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.booth.PODCAST_NOTIFICATION_ACTION_ADD_TO_QUEUE
import com.shapeshed.booth.PODCAST_NOTIFICATION_ACTION_PLAY

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
    // Both effects below are keyed on data, not on these callbacks, so neither should restart when
    // the parent recreates them. Reading them through updated state keeps them current.
    val currentOnOpenInitialEpisode by rememberUpdatedState(onOpenInitialEpisode)
    val currentOnSelectSavedTab by rememberUpdatedState(onSelectSavedTab)
    // Refresh is scheduled per feed from its own publishing pattern, so there is no interval to key
    // on and nothing to re-key when settings change. This only makes sure the schedule exists when
    // the app opens; each feed re-arms itself after it is fetched, and a periodic pass repairs
    // anything that went missing while the app was closed.
    LaunchedEffect(Unit) { viewModel.ensureRefreshSchedule(context) }
    LaunchedEffect(Unit) { playbackViewModel.connect(context) }
    // Warm the selected catalogue while the home screen is settling. The locale is part of
    // the key so changing the app language cannot leave the previous region's shelf visible.
    LaunchedEffect(context.resources.configuration.locales[0]?.toLanguageTag()) {
        viewModel.loadDiscovery(force = true)
    }
    LaunchedEffect(initialEpisodeId, initialNotificationAction) {
        initialEpisodeId?.let { episodeId ->
            viewModel.episode(episodeId)?.let { episode ->
                currentOnOpenInitialEpisode(episode)
                when (initialNotificationAction) {
                    PODCAST_NOTIFICATION_ACTION_ADD_TO_QUEUE -> viewModel.addToQueueFromInbox(episode.id)

                    PODCAST_NOTIFICATION_ACTION_PLAY -> {
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
            ?.let(currentOnSelectSavedTab)
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

/**
 * The back gestures, and the one thing the now-playing one has to do beyond dismissing it.
 *
 * Takes that as a callback rather than the ViewModel, which it needed for exactly this one call.
 */
@Composable
internal fun PodcastHomeBackHandlers(
    routeState: PodcastHomeRouteState,
    onSwitchToAudioForBackground: () -> Unit,
    inboxSelectionMode: Boolean,
    showGlobalSearch: Boolean,
    onCloseGlobalSearch: () -> Unit,
) {
    BackHandler(enabled = routeState.showNowPlaying.value) {
        onSwitchToAudioForBackground()
        routeState.showNowPlaying.value = false
    }
    BackHandler(enabled = routeState.queueReorderMode.value) {
        routeState.queueReorderMode.value = false
    }
    BackHandler(enabled = inboxSelectionMode) { routeState.selectedInboxIds.value = emptySet() }
    BackHandler(enabled = showGlobalSearch) { onCloseGlobalSearch() }
    BackHandler(enabled = !routeState.showNowPlaying.value && routeState.showDiscoverySearch.value) {
        routeState.showDiscoverySearch.value = false
    }
}
