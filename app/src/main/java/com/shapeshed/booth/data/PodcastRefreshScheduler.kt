package com.shapeshed.booth.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shapeshed.booth.di.boothWorkerEntryPoint
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Gives every subscription its own refresh, scheduled for when that feed is next due.
 *
 * The app previously ran one periodic worker over the whole library on a listener-chosen interval.
 * That coupled two unrelated things: how often any feed was checked, and how often any particular
 * feed was checked. A daily briefing and a fortnightly magazine could not both be served by one
 * number, and the only way to catch a morning edition was to poll everything often enough, which
 * is also the only way to waste battery on a feed that publishes twice a year.
 *
 * So each feed gets a one-time request scheduled at the moment its own pattern says to look. A
 * weekly show wakes the app once a week; the daily briefing once a day. Nothing polls.
 *
 * WorkManager will not hold a pending request for more than about a month, so a periodic
 * reconciliation pass re-arms everything periodically. It fetches nothing; it only notices
 * subscriptions whose scheduled refresh has gone missing, which happens after a subscribe, a reboot
 * that pruned pending work, or a clock change.
 */
object PodcastRefreshScheduler {
    private const val RECONCILE_WORK_NAME = "podcast-refresh-reconcile"

    /** Feeds are small text documents, so any connection is fine and nothing waits for Wi-Fi. */
    private fun constraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun workName(podcastId: Long) = "podcast-refresh-$podcastId"

    /**
     * Schedules [podcastId] for its next due time, replacing anything already queued for it.
     *
     * Replace rather than keep, because the answer changes as the feed's pattern becomes clearer and
     * a request left over from a sparser history would be checked at the wrong time.
     */
    suspend fun schedule(context: Context, podcastId: Long, nowMillis: Long = System.currentTimeMillis()) {
        val repository = boothWorkerEntryPoint(context).podcastRepository
        val podcast = repository.podcast(podcastId) ?: return
        if (!podcast.isSubscribed) {
            cancel(context, podcastId)
            return
        }
        val dueAt = repository.nextRefreshDueMillis(podcast, nowMillis)
        val delayMillis = (dueAt - nowMillis).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<PodcastRefreshWorker>()
            .setInputData(workDataOf(PodcastRefreshWorker.PODCAST_ID_INPUT to podcastId))
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(PodcastRefreshWorker.REFRESH_WORK_TAG)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            workName(podcastId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /** Schedules every current subscription. Used on subscribe and by the reconciliation pass. */
    suspend fun scheduleAll(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val repository = boothWorkerEntryPoint(context).podcastRepository
        repository.podcasts.first().forEach { podcast ->
            schedule(context, podcast.id, nowMillis)
        }
    }

    fun cancel(context: Context, podcastId: Long) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(workName(podcastId))
    }

    /**
     * Keeps the reconciliation pass itself scheduled.
     *
     * Idempotent, and deliberately not keyed on any setting: there is no longer a refresh interval
     * for the listener to change, so nothing needs to re-key this when settings change.
     */
    fun ensureReconciliation(context: Context) {
        val request = PeriodicWorkRequestBuilder<PodcastRefreshWorker>(
            RefreshCadence.RECONCILE_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS,
        )
            .setConstraints(constraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(PodcastRefreshWorker.REFRESH_WORK_TAG)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            RECONCILE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
