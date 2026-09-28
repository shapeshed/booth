package com.shapeshed.booth.data

data class PodcastSettingsState(
    val downloadVideos: Boolean = false,
    val refreshInterval: PodcastRefreshInterval = PodcastRefreshInterval.SIX_HOURS,
    val refreshNetwork: PodcastRefreshNetwork = PodcastRefreshNetwork.ANY_CONNECTION,
    val notificationsEnabled: Boolean = false,
    val autoQueueEnabled: Boolean = false,
    val downloadEpisodesAddedToUpNext: Boolean = false,
    val downloadNetwork: PodcastDownloadNetwork = PodcastDownloadNetwork.WIFI_ONLY,
    val removePlayedDownloads: Boolean = true,
    val savedPodcastTab: String? = null,
    val searchProviderId: String = "apple",
)
