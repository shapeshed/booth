package com.shapeshed.booth.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PodcastSubscriptionStage { QUEUED, FETCHING, PARSING, SAVING, UNSUBSCRIBING, ADDED, FAILED }

internal val PodcastSubscriptionStage.isInProgress: Boolean
    get() = this in setOf(
        PodcastSubscriptionStage.QUEUED,
        PodcastSubscriptionStage.FETCHING,
        PodcastSubscriptionStage.PARSING,
        PodcastSubscriptionStage.SAVING,
        PodcastSubscriptionStage.UNSUBSCRIBING,
    )

internal val PodcastSubscriptionStage.isAdded: Boolean
    get() = this == PodcastSubscriptionStage.ADDED

data class PodcastSubscriptionProgress(
    val stage: PodcastSubscriptionStage,
    val title: String? = null,
    val processedEpisodes: Int? = null,
    val totalEpisodes: Int? = null,
    val errorMessage: String? = null,
)

object PodcastSubscriptionProgressStore {
    private val _progress = MutableStateFlow<Map<String, PodcastSubscriptionProgress>>(emptyMap())
    val progress: StateFlow<Map<String, PodcastSubscriptionProgress>> = _progress.asStateFlow()

    fun queued(feedUrl: String, title: String? = null) = update(
        feedUrl,
        PodcastSubscriptionProgress(PodcastSubscriptionStage.QUEUED, title = title),
    )

    fun update(feedUrl: String, value: PodcastSubscriptionProgress) {
        _progress.value = _progress.value + (feedUrl to value)
    }

    fun clear(feedUrl: String) {
        _progress.value = _progress.value - feedUrl
    }
}
