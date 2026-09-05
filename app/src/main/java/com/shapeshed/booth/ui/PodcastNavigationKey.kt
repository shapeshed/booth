package com.shapeshed.booth.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Persistable destinations for the podcast surface.
 *
 * These keys are deliberately data-only. Screens remain responsible for loading their own
 * state, while Navigation 3 owns the back stack and can restore it after process recreation.
 */
@Serializable
internal sealed interface PodcastNavigationKey : NavKey {
    @Serializable
    data object Inbox : PodcastNavigationKey

    @Serializable
    data object UpNext : PodcastNavigationKey

    @Serializable
    data object Subscriptions : PodcastNavigationKey

    @Serializable
    data object Settings : PodcastNavigationKey

    @Serializable
    data class PodcastManagement(val category: PodcastManagementCategory) : PodcastNavigationKey

    @Serializable
    data object GlobalSearch : PodcastNavigationKey

    @Serializable
    data object Discovery : PodcastNavigationKey

    @Serializable
    data class DiscoveryPodcast(val feedUrl: String) : PodcastNavigationKey

    @Serializable
    data class DiscoveryCategory(
        val providerId: String,
        val categoryId: String,
        val title: String,
    ) : PodcastNavigationKey

    @Serializable
    data class DiscoveryEpisode(val episodeId: Long) : PodcastNavigationKey

    @Serializable
    data class PodcastDetail(
        val podcastId: Long,
    ) : PodcastNavigationKey

    @Serializable
    data class PodcastSettings(
        val podcastId: Long,
    ) : PodcastNavigationKey

    @Serializable
    data class EpisodeDetail(
        val episodeId: Long,
        val origin: EpisodeNavigationOrigin = EpisodeNavigationOrigin.Inbox,
    ) : PodcastNavigationKey

    @Serializable
    data class Category(
        val providerId: String,
        val categoryId: String,
        val title: String,
    ) : PodcastNavigationKey

    @Serializable
    data object AllEpisodes : PodcastNavigationKey

    @Serializable
    data object Downloads : PodcastNavigationKey

    @Serializable
    data object NowPlaying : PodcastNavigationKey

    @Serializable
    data object NowPlayingQueue : PodcastNavigationKey
}

@Serializable
internal enum class PodcastManagementCategory {
    PLAYBACK_SPEED,
    AUTO_REFRESH,
    AUTO_DOWNLOAD,
    AUTO_QUEUE,
    VIDEO_DOWNLOAD,
    NOTIFICATIONS,
}

internal fun PodcastTab.toNavigationKey(): PodcastNavigationKey = when (this) {
    PodcastTab.HOME -> PodcastNavigationKey.Inbox
    PodcastTab.UP_NEXT -> PodcastNavigationKey.UpNext
    PodcastTab.SUBSCRIPTIONS -> PodcastNavigationKey.Subscriptions
}

internal fun PodcastNavigationKey.toPodcastTab(): PodcastTab? = when (this) {
    PodcastNavigationKey.Inbox -> PodcastTab.HOME
    PodcastNavigationKey.UpNext -> PodcastTab.UP_NEXT
    PodcastNavigationKey.Subscriptions -> PodcastTab.SUBSCRIPTIONS
    else -> null
}

@Serializable
internal enum class EpisodeNavigationOrigin {
    Inbox,
    UpNext,
    Downloads,
    AllEpisodes,
    Podcast,
    Search,
}
