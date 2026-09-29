package com.shapeshed.booth.data

data class PodcastSettingsState(
    /**
     * The app-wide playback speed, and the default for any podcast that has not set its own.
     *
     * A podcast's own speed is nullable and null means follow this, so this is the fallback rather
     * than a value every podcast has to agree with. It is mutable because it is read once on
     * connect and per media item transition, not observed.
     */
    val globalPlaybackSpeed: Float = 1f,
    val globalSkipSilence: Boolean = false,
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
