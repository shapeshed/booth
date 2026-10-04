package com.shapeshed.booth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class PodcastSubscriptionsViewMode { LIST, GRID }

enum class PodcastDownloadNetwork { ANY_CONNECTION, WIFI_ONLY }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Podcast-only persisted settings backed by Preferences DataStore. */
class SettingsStore(private val context: Context) {
    private val subscriptionsViewModeKey = stringPreferencesKey("podcast_subscriptions_view_mode")
    private val selectedTabKey = stringPreferencesKey("podcast_selected_tab")
    private val lastEpisodeIdKey = longPreferencesKey("podcast_last_episode_id")
    private val playbackSpeedKey = floatPreferencesKey("podcast_playback_speed")
    private val skipSilenceKey = booleanPreferencesKey("podcast_skip_silence")
    private val notificationsEnabledKey = booleanPreferencesKey("podcast_notifications_enabled")
    private val autoQueueEnabledKey = booleanPreferencesKey("podcast_auto_queue_enabled")
    private val downloadEpisodesAddedToUpNextKey = booleanPreferencesKey("podcast_download_episodes_added_to_up_next")
    private val downloadNetworkKey = stringPreferencesKey("podcast_download_network")
    private val removePlayedDownloadsKey = booleanPreferencesKey("podcast_remove_played_downloads")
    private val downloadVideosKey = booleanPreferencesKey("podcast_download_videos")
    private val searchProviderKey = stringPreferencesKey("podcast_search_provider")
    private val sleepTimerRemainingMsKey = longPreferencesKey("podcast_sleep_timer_remaining_ms")
    private val sleepTimerTotalMsKey = longPreferencesKey("podcast_sleep_timer_total_ms")
    private val lastInboxViewedAtMillisKey = longPreferencesKey("podcast_last_inbox_viewed_at_millis")

    val podcastSubscriptionsViewMode: Flow<PodcastSubscriptionsViewMode> = context.dataStore.data.map { prefs ->
        prefs[subscriptionsViewModeKey]
            ?.let { runCatching { PodcastSubscriptionsViewMode.valueOf(it) }.getOrNull() }
            ?: PodcastSubscriptionsViewMode.LIST
    }

    suspend fun setPodcastSubscriptionsViewMode(mode: PodcastSubscriptionsViewMode) {
        context.dataStore.edit { it[subscriptionsViewModeKey] = mode.name }
    }

    val podcastSelectedTab: Flow<String?> = context.dataStore.data.map { it[selectedTabKey] }

    suspend fun setPodcastSelectedTab(tab: String) {
        context.dataStore.edit { it[selectedTabKey] = tab }
    }

    /**
     * When the Inbox was last looked at, used to decide whether the tab badge is shown.
     *
     * Defaults to 0 rather than to "now" so a fresh install counts everything already in the Inbox as
     * unseen and shows the dot, which is the honest state: the user has not seen any of it.
     */
    val lastInboxViewedAtMillis: Flow<Long> = context.dataStore.data.map {
        it[lastInboxViewedAtMillisKey] ?: 0L
    }

    suspend fun setLastInboxViewedAtMillis(millis: Long) {
        context.dataStore.edit { it[lastInboxViewedAtMillisKey] = millis }
    }

    val podcastLastEpisodeId: Flow<Long?> = context.dataStore.data.map { it[lastEpisodeIdKey] }

    /**
     * The sleep timer as it stood when the process last ran.
     *
     * Persisted because the timer lives in the media service, and a process death mid-playback
     * would otherwise silently drop it: the service restarts, playback resumes, and the user
     * listens indefinitely past the point they asked to stop at. The value is the remaining
     * *playback* time, not a wall-clock deadline, because paused time must not count against it.
     */
    val sleepTimer: Flow<SleepTimerState?> = context.dataStore.data.map { prefs ->
        val total = prefs[sleepTimerTotalMsKey] ?: return@map null
        val remaining = prefs[sleepTimerRemainingMsKey] ?: return@map null
        SleepTimerState(totalMs = total, remainingMs = remaining)
    }

    suspend fun setSleepTimer(state: SleepTimerState) {
        context.dataStore.edit {
            it[sleepTimerTotalMsKey] = state.totalMs
            it[sleepTimerRemainingMsKey] = state.remainingMs
        }
    }

    suspend fun clearSleepTimer() {
        context.dataStore.edit {
            it.remove(sleepTimerTotalMsKey)
            it.remove(sleepTimerRemainingMsKey)
        }
    }

    suspend fun setPodcastLastEpisodeId(episodeId: Long) {
        context.dataStore.edit { it[lastEpisodeIdKey] = episodeId }
    }

    suspend fun clearPodcastLastEpisodeId() {
        context.dataStore.edit { it.remove(lastEpisodeIdKey) }
    }

    val podcastPlaybackSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        PlaybackSettings.clampSpeed(prefs[playbackSpeedKey] ?: PlaybackSettings.DEFAULT_SPEED)
    }

    suspend fun setPodcastPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[playbackSpeedKey] = PlaybackSettings.clampSpeed(speed) }
    }

    val podcastSkipSilence: Flow<Boolean> = context.dataStore.data.map { it[skipSilenceKey] ?: false }

    suspend fun setPodcastSkipSilence(enabled: Boolean) {
        context.dataStore.edit { it[skipSilenceKey] = enabled }
    }

    val podcastNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[notificationsEnabledKey] ?: false }

    suspend fun setPodcastNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[notificationsEnabledKey] = enabled }
    }

    val podcastAutoQueueEnabled: Flow<Boolean> = context.dataStore.data.map { it[autoQueueEnabledKey] ?: false }

    suspend fun setPodcastAutoQueueEnabled(enabled: Boolean) {
        context.dataStore.edit { it[autoQueueEnabledKey] = enabled }
    }

    val podcastDownloadEpisodesAddedToUpNext: Flow<Boolean> = context.dataStore.data.map {
        it[downloadEpisodesAddedToUpNextKey] ?: false
    }

    suspend fun setPodcastDownloadEpisodesAddedToUpNext(enabled: Boolean) {
        context.dataStore.edit { it[downloadEpisodesAddedToUpNextKey] = enabled }
    }

    val podcastDownloadNetwork: Flow<PodcastDownloadNetwork> = context.dataStore.data.map { prefs ->
        prefs[downloadNetworkKey]
            ?.let { runCatching { PodcastDownloadNetwork.valueOf(it) }.getOrNull() }
            ?: PodcastDownloadNetwork.WIFI_ONLY
    }

    suspend fun setPodcastDownloadNetwork(network: PodcastDownloadNetwork) {
        context.dataStore.edit { it[downloadNetworkKey] = network.name }
    }

    val podcastRemovePlayedDownloads: Flow<Boolean> = context.dataStore.data.map {
        it[removePlayedDownloadsKey] ?: true
    }

    suspend fun setPodcastRemovePlayedDownloads(enabled: Boolean) {
        context.dataStore.edit { it[removePlayedDownloadsKey] = enabled }
    }

    val podcastDownloadVideos: Flow<Boolean> = context.dataStore.data.map { it[downloadVideosKey] ?: false }

    suspend fun setPodcastDownloadVideos(enabled: Boolean) {
        context.dataStore.edit { it[downloadVideosKey] = enabled }
    }

    val podcastSearchProvider: Flow<String> = context.dataStore.data.map {
        it[searchProviderKey]
            ?: DEFAULT_SEARCH_PROVIDER
    }

    suspend fun setPodcastSearchProvider(providerId: String) {
        context.dataStore.edit { it[searchProviderKey] = providerId }
    }

    private companion object {
        const val DEFAULT_SEARCH_PROVIDER = "apple"
    }
}
