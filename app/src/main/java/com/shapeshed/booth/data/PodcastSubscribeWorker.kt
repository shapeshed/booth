package com.shapeshed.booth.data

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.shapeshed.booth.BoothApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

const val SUBSCRIBE_FEED_URL_INPUT = "feedUrl"
const val SUBSCRIBE_TITLE_INPUT = "title"
const val SUBSCRIBE_DESCRIPTION_INPUT = "descriptionHtml"
const val SUBSCRIBE_APPLE_CATEGORIES_INPUT = "appleCategories"
const val SUBSCRIBE_APPLE_CATEGORY_IDS_INPUT = "appleCategoryIds"
const val SUBSCRIBE_CATEGORY_PROVIDER_ID_INPUT = "categoryProviderId"

/** Parses and persists one subscription away from the search/UI coroutine. */
class PodcastSubscribeWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val feedUrl = inputData.getString(SUBSCRIBE_FEED_URL_INPUT)
            ?.takeIf(String::isNotBlank)
            ?: return@withContext Result.failure()
        val requestedTitle = inputData.getString(SUBSCRIBE_TITLE_INPUT)
        val fallbackDescriptionHtml = inputData.getString(SUBSCRIBE_DESCRIPTION_INPUT)
        val appleCategories = inputData.getString(SUBSCRIBE_APPLE_CATEGORIES_INPUT)
            ?.let(::decodeAppleCategories)
            .orEmpty()
        val appleCategoryIds = inputData.getString(SUBSCRIBE_APPLE_CATEGORY_IDS_INPUT)
            ?.let(::decodeAppleCategoryIds)
            .orEmpty()
        val categoryProviderId = inputData.getString(SUBSCRIBE_CATEGORY_PROVIDER_ID_INPUT)
            ?.takeIf(String::isNotBlank)
            ?: APPLE_DIRECTORY_PROVIDER_ID
        val app = applicationContext as BoothApp
        try {
            // A retained preview may contain only episodes explicitly selected by the user.
            // Always complete a fresh parse before promoting a podcast to a subscription.
            // This keeps streamed discovery chunks from becoming an incomplete subscription.
            PodcastSubscriptionProgressStore.update(
                feedUrl,
                PodcastSubscriptionProgress(PodcastSubscriptionStage.FETCHING, title = requestedTitle),
            )
            val feed = app.podcastRepository.subscribe(
                feedUrl,
                fallbackDescriptionHtml = fallbackDescriptionHtml,
                appleCategories = appleCategories,
                appleCategoryIds = appleCategoryIds,
                categoryProviderId = categoryProviderId,
                onEpisodeProgress = { processed, total ->
                    PodcastSubscriptionProgressStore.update(
                        feedUrl,
                        PodcastSubscriptionProgress(
                            PodcastSubscriptionStage.PARSING,
                            title = requestedTitle,
                            processedEpisodes = processed,
                            totalEpisodes = total,
                        ),
                    )
                },
                onSaving = {
                    PodcastSubscriptionProgressStore.update(
                        feedUrl,
                        PodcastSubscriptionProgress(PodcastSubscriptionStage.SAVING, title = requestedTitle),
                    )
                },
            )
            PodcastSubscriptionProgressStore.update(
                feedUrl,
                PodcastSubscriptionProgress(
                    PodcastSubscriptionStage.ADDED,
                    title = feed.podcast.title.takeIf(String::isNotBlank) ?: requestedTitle,
                ),
            )
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (runAttemptCount >= MAX_ATTEMPTS - 1) {
                Log.w(TAG, "Subscription failed after $MAX_ATTEMPTS attempts: $feedUrl", error)
                PodcastSubscriptionProgressStore.update(
                    feedUrl,
                    PodcastSubscriptionProgress(
                        stage = PodcastSubscriptionStage.FAILED,
                        title = requestedTitle,
                        errorMessage = error.message,
                    ),
                )
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "PodcastSubscribeWorker"
        private const val MAX_ATTEMPTS = 3
    }
}
