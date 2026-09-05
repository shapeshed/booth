package com.shapeshed.booth.data

data class PodcastSettingsState(
    val downloadVideos: Boolean = false,
    val autoRefreshEnabled: Boolean = false,
    val refreshInterval: PodcastRefreshInterval = PodcastRefreshInterval.SIX_HOURS,
    val refreshNetwork: PodcastRefreshNetwork = PodcastRefreshNetwork.WIFI_ONLY,
    val notificationsEnabled: Boolean = false,
    val autoDownloadEnabled: Boolean = false,
    val autoQueueEnabled: Boolean = false,
    val downloadNetwork: PodcastDownloadNetwork = PodcastDownloadNetwork.WIFI_ONLY,
    val savedPodcastTab: String? = null,
    val searchProviderId: String = "apple",
)
