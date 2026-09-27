package com.shapeshed.booth.data

import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

/**
 * In-flight subscription progress, keyed by feed URL.
 *
 * A class rather than an `object` for the same reason as DownloadProgressStore: it is written
 * from Dispatchers.IO in PodcastSubscribeWorker and from the main thread in the ViewModel, so it
 * should be visible in the object graph and replaceable in a test.
 */
class PodcastSubscriptionProgressStore @Inject constructor() {
    private val _progress = MutableStateFlow<Map<String, PodcastSubscriptionProgress>>(emptyMap())
    val progress: StateFlow<Map<String, PodcastSubscriptionProgress>> = _progress.asStateFlow()

    fun queued(feedUrl: String, title: String? = null) = update(
        feedUrl,
        PodcastSubscriptionProgress(PodcastSubscriptionStage.QUEUED, title = title),
    )

    // Written from Dispatchers.IO in PodcastSubscribeWorker and from the main thread in the
    // ViewModel, so the read-modify-write must be a compare-and-set. A lost update here strands
    // a subscription spinner or hides an in-flight subscription.
    fun update(feedUrl: String, value: PodcastSubscriptionProgress) {
        _progress.update { it + (feedUrl to value) }
    }

    fun clear(feedUrl: String) {
        _progress.update { it - feedUrl }
    }
}
