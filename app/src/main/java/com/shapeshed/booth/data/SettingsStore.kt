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

enum class PodcastRefreshInterval(val minutes: Long) {
    HOURLY(60),
    SIX_HOURS(360),
    DAILY(1_440),
}

enum class PodcastRefreshNetwork { ANY_CONNECTION, WIFI_ONLY }

enum class PodcastDownloadNetwork { ANY_CONNECTION, WIFI_ONLY }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Podcast-only persisted settings backed by Preferences DataStore. */
class SettingsStore(private val context: Context) {
    private val subscriptionsViewModeKey = stringPreferencesKey("podcast_subscriptions_view_mode")
    private val selectedTabKey = stringPreferencesKey("podcast_selected_tab")
    private val lastEpisodeIdKey = longPreferencesKey("podcast_last_episode_id")
    private val playbackSpeedKey = floatPreferencesKey("podcast_playback_speed")
    private val skipSilenceKey = booleanPreferencesKey("podcast_skip_silence")
    private val autoRefreshEnabledKey = booleanPreferencesKey("podcast_auto_refresh_enabled")
    private val refreshIntervalKey = stringPreferencesKey("podcast_refresh_interval")
    private val refreshNetworkKey = stringPreferencesKey("podcast_refresh_network")
    private val notificationsEnabledKey = booleanPreferencesKey("podcast_notifications_enabled")
    private val autoDownloadEnabledKey = booleanPreferencesKey("podcast_auto_download_enabled")
    private val autoQueueEnabledKey = booleanPreferencesKey("podcast_auto_queue_enabled")
    private val downloadNetworkKey = stringPreferencesKey("podcast_download_network")
    private val downloadVideosKey = booleanPreferencesKey("podcast_download_videos")
    private val searchProviderKey = stringPreferencesKey("podcast_search_provider")

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

    val podcastLastEpisodeId: Flow<Long?> = context.dataStore.data.map { it[lastEpisodeIdKey] }

    suspend fun setPodcastLastEpisodeId(episodeId: Long) {
        context.dataStore.edit { it[lastEpisodeIdKey] = episodeId }
    }

    suspend fun clearPodcastLastEpisodeId() {
        context.dataStore.edit { it.remove(lastEpisodeIdKey) }
    }

    val podcastPlaybackSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        (prefs[playbackSpeedKey] ?: DEFAULT_PLAYBACK_SPEED).coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
    }

    suspend fun setPodcastPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[playbackSpeedKey] = speed.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED) }
    }

    val podcastSkipSilence: Flow<Boolean> = context.dataStore.data.map { it[skipSilenceKey] ?: false }

    suspend fun setPodcastSkipSilence(enabled: Boolean) {
        context.dataStore.edit { it[skipSilenceKey] = enabled }
    }

    val podcastAutoRefreshEnabled: Flow<Boolean> = context.dataStore.data.map { it[autoRefreshEnabledKey] ?: false }

    suspend fun setPodcastAutoRefreshEnabled(enabled: Boolean) {
        context.dataStore.edit { it[autoRefreshEnabledKey] = enabled }
    }

    val podcastRefreshInterval: Flow<PodcastRefreshInterval> = context.dataStore.data.map { prefs ->
        prefs[refreshIntervalKey]
            ?.let { runCatching { PodcastRefreshInterval.valueOf(it) }.getOrNull() }
            ?: PodcastRefreshInterval.SIX_HOURS
    }

    suspend fun setPodcastRefreshInterval(interval: PodcastRefreshInterval) {
        context.dataStore.edit { it[refreshIntervalKey] = interval.name }
    }

    val podcastRefreshNetwork: Flow<PodcastRefreshNetwork> = context.dataStore.data.map { prefs ->
        prefs[refreshNetworkKey]
            ?.let { runCatching { PodcastRefreshNetwork.valueOf(it) }.getOrNull() }
            ?: PodcastRefreshNetwork.WIFI_ONLY
    }

    suspend fun setPodcastRefreshNetwork(network: PodcastRefreshNetwork) {
        context.dataStore.edit { it[refreshNetworkKey] = network.name }
    }

    val podcastNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[notificationsEnabledKey] ?: false }

    suspend fun setPodcastNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[notificationsEnabledKey] = enabled }
    }

    val podcastAutoDownloadEnabled: Flow<Boolean> = context.dataStore.data.map { it[autoDownloadEnabledKey] ?: false }

    suspend fun setPodcastAutoDownloadEnabled(enabled: Boolean) {
        context.dataStore.edit { it[autoDownloadEnabledKey] = enabled }
    }

    val podcastAutoQueueEnabled: Flow<Boolean> = context.dataStore.data.map { it[autoQueueEnabledKey] ?: false }

    suspend fun setPodcastAutoQueueEnabled(enabled: Boolean) {
        context.dataStore.edit { it[autoQueueEnabledKey] = enabled }
    }

    val podcastDownloadNetwork: Flow<PodcastDownloadNetwork> = context.dataStore.data.map { prefs ->
        prefs[downloadNetworkKey]
            ?.let { runCatching { PodcastDownloadNetwork.valueOf(it) }.getOrNull() }
            ?: PodcastDownloadNetwork.WIFI_ONLY
    }

    suspend fun setPodcastDownloadNetwork(network: PodcastDownloadNetwork) {
        context.dataStore.edit { it[downloadNetworkKey] = network.name }
    }

    val podcastDownloadVideos: Flow<Boolean> = context.dataStore.data.map { it[downloadVideosKey] ?: false }

    suspend fun setPodcastDownloadVideos(enabled: Boolean) {
        context.dataStore.edit { it[downloadVideosKey] = enabled }
    }

    val podcastSearchProvider: Flow<String> = context.dataStore.data.map { it[searchProviderKey] ?: DEFAULT_SEARCH_PROVIDER }

    suspend fun setPodcastSearchProvider(providerId: String) {
        context.dataStore.edit { it[searchProviderKey] = providerId }
    }

    private companion object {
        const val DEFAULT_PLAYBACK_SPEED = 1f
        const val MIN_PLAYBACK_SPEED = 0.5f
        const val MAX_PLAYBACK_SPEED = 3f
        const val DEFAULT_SEARCH_PROVIDER = "apple"
    }
}
