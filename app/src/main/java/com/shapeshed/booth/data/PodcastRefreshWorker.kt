package com.shapeshed.booth.data

import android.Manifest
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.content.Context
import java.io.ByteArrayOutputStream
import androidx.core.app.NotificationCompat
import androidx.core.graphics.scale
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shapeshed.booth.R
import com.shapeshed.booth.ExtraInitialPodcastEpisodeId
import com.shapeshed.booth.ExtraInitialPodcastNotificationAction
import com.shapeshed.booth.PodcastNotificationActionAddToQueue
import com.shapeshed.booth.PodcastNotificationActionPlay
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.InputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class PodcastRefreshWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val app = applicationContext as com.shapeshed.booth.BoothApp
        if (!app.settings.podcastAutoRefreshEnabled.first()) return Result.success()
        var retryableFailure = false
        val notificationsEnabled = app.settings.podcastNotificationsEnabled.first()
        val autoDownloadEnabled = app.settings.podcastAutoDownloadEnabled.first()
        val autoQueueEnabled = app.settings.podcastAutoQueueEnabled.first()
        val downloadNetwork = app.settings.podcastDownloadNetwork.first()
        val newEpisodes = mutableListOf<NewPodcastEpisodeNotification>()
        val podcasts = app.podcastRepository.podcasts.first().filter { it.includeInAutoRefresh }
        val existingGuids = podcasts.associate { podcast ->
            podcast.id to app.podcastRepository.episodeGuids(podcast.id).toHashSet()
        }
        val refreshResults = app.podcastRepository.refreshAll(podcasts)
        refreshResults.forEach { refreshResult ->
            currentCoroutineContext().ensureActive()
            val podcast = refreshResult.podcast
            val error = refreshResult.result.exceptionOrNull()
            if (error != null) {
                // One unavailable feed should not prevent all other subscriptions from updating.
                // Permanent feed errors are left for the next scheduled refresh; only transient
                // network/server failures should cause WorkManager to retry the batch.
                if (error.isRetryableFeedFailure()) retryableFailure = true
                return@forEach
            }
            // Use persisted rows: repository identity normalization can differ from the
            // parser's provisional IDs after redirects or feed URL changes.
            val newPersistedEpisodes = app.podcastRepository.episodes(podcast.id).first()
                .filterNot { it.guid in existingGuids[podcast.id].orEmpty() }
            if (autoQueueEnabled && podcast.includeInAutoQueue) {
                newPersistedEpisodes.forEach { episode ->
                    app.podcastRepository.addToQueueFromInbox(episode.id)
                }
            }
            if (autoDownloadEnabled && podcast.includeInAutoDownload) {
                newPersistedEpisodes.forEach { episode ->
                    val request = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
                        .setInputData(workDataOf(EPISODE_ID_INPUT to episode.id))
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(
                                    if (downloadNetwork == PodcastDownloadNetwork.WIFI_ONLY) {
                                        NetworkType.UNMETERED
                                    } else {
                                        NetworkType.CONNECTED
                                    },
                                )
                                .build(),
                        )
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                        .build()
                    WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                        "podcast-download-${episode.id}",
                        androidx.work.ExistingWorkPolicy.KEEP,
                        request,
                    )
                }
            }
            if (notificationsEnabled && podcast.includeInNotifications) {
                newPersistedEpisodes
                    .take(10)
                    .forEach { episode ->
                        newEpisodes += NewPodcastEpisodeNotification(
                            episodeId = episode.id,
                            podcastTitle = podcast.title,
                            episodeTitle = episode.title,
                            artworkUrl = episode.artworkUrl ?: podcast.artworkUrl,
                        )
                    }
            }
        }
        if (newEpisodes.isNotEmpty()) {
            applicationContext.postPodcastNotifications(newEpisodes, app.okHttpClient)
        }
        return if (retryableFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val WorkName = "podcast-auto-refresh"

        fun schedule(
            context: Context,
            enabled: Boolean,
            interval: PodcastRefreshInterval,
            network: PodcastRefreshNetwork,
        ) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            if (!enabled) {
                workManager.cancelUniqueWork(WorkName)
                return
            }
            val networkType = when (network) {
                PodcastRefreshNetwork.ANY_CONNECTION -> NetworkType.CONNECTED
                PodcastRefreshNetwork.WIFI_ONLY -> NetworkType.UNMETERED
            }
            val request = PeriodicWorkRequestBuilder<PodcastRefreshWorker>(interval.minutes, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(networkType).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                WorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}

internal fun Throwable.isRetryableFeedFailure(): Boolean = when (this) {
    is FeedHttpException -> statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode in 500..599
    is SocketTimeoutException, is ConnectException, is IOException -> true
    else -> false
}

private const val PodcastNotificationChannelId = "podcast_new_episodes"
private const val PodcastNotificationSummaryId = 2201
private const val PodcastNotificationChildIdBase = 2202
private const val PodcastNotificationGroup = "podcast_new_episodes"

private data class NewPodcastEpisodeNotification(
    val episodeId: Long,
    val podcastTitle: String,
    val episodeTitle: String,
    val artworkUrl: String?,
)

private suspend fun Context.postPodcastNotifications(
    episodes: List<NewPodcastEpisodeNotification>,
    client: okhttp3.OkHttpClient,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return

    val manager = getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(PodcastNotificationChannelId) == null) {
        manager.createNotificationChannel(
            NotificationChannel(
                PodcastNotificationChannelId,
                getString(R.string.podcast_notifications_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.podcast_notifications_channel_description)
            },
        )
    }

    val uniqueEpisodes = episodes.distinctBy(NewPodcastEpisodeNotification::episodeId)
    val title = resources.getQuantityString(
        R.plurals.new_podcast_episodes,
        uniqueEpisodes.size,
        uniqueEpisodes.size,
    )
    uniqueEpisodes.forEach { episode ->
        val contentIntent = PendingIntent.getActivity(
            this,
            episode.episodeId.hashCode(),
            Intent(this, com.shapeshed.booth.MainActivity::class.java).apply {
                putExtra(ExtraInitialPodcastEpisodeId, episode.episodeId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fun actionIntent(action: String): PendingIntent = PendingIntent.getActivity(
            this,
            (episode.episodeId.hashCode() * 31) + action.hashCode(),
            Intent(this, com.shapeshed.booth.MainActivity::class.java).apply {
                putExtra(ExtraInitialPodcastEpisodeId, episode.episodeId)
                putExtra(ExtraInitialPodcastNotificationAction, action)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val artwork = episode.artworkUrl?.let { loadNotificationBitmap(client, it) }
        val builder = NotificationCompat.Builder(this, PodcastNotificationChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(episode.episodeTitle)
            .setContentText(episode.podcastTitle)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(PodcastNotificationGroup)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .addAction(
                R.drawable.ic_notification_play,
                getString(R.string.notification_action_play),
                actionIntent(PodcastNotificationActionPlay),
            )
            .addAction(
                R.drawable.ic_notification_queue,
                getString(R.string.notification_action_add_to_queue),
                actionIntent(PodcastNotificationActionAddToQueue),
            )
        if (artwork != null) {
            builder
                .setLargeIcon(artwork)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(artwork)
                        .bigLargeIcon(null as Bitmap?),
                )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(episode.podcastTitle))
        }
        manager.notify(PodcastNotificationChildIdBase + episode.episodeId.hashCode(), builder.build())
    }

    val summaryIntent = PendingIntent.getActivity(
        this,
        PodcastNotificationSummaryId,
        Intent(this, com.shapeshed.booth.MainActivity::class.java).apply {
            putExtra(ExtraInitialPodcastEpisodeId, uniqueEpisodes.first().episodeId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val inboxStyle = NotificationCompat.InboxStyle()
        .setSummaryText(title)
        .also { style -> uniqueEpisodes.take(5).forEach { style.addLine("${it.podcastTitle} · ${it.episodeTitle}") } }
    manager.notify(
        PodcastNotificationSummaryId,
        NotificationCompat.Builder(this, PodcastNotificationChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(uniqueEpisodes.first().episodeTitle)
            .setContentIntent(summaryIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(PodcastNotificationGroup)
            .setGroupSummary(true)
            .setStyle(inboxStyle)
            .build(),
    )
}

private suspend fun loadNotificationBitmap(
    client: okhttp3.OkHttpClient,
    imageUrl: String,
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        client.newCall(Request.Builder().url(imageUrl).build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val bytes = response.body.byteStream().use {
                readBoundedBytes(it, MaxNotificationImageBytes)
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@use null
            val options = BitmapFactory.Options().apply {
                inSampleSize = notificationSampleSize(bounds.outWidth, bounds.outHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.fitNotificationSize()
        }
    }.getOrNull()
}

internal fun readBoundedBytes(input: InputStream, maxBytes: Long): ByteArray {
    require(maxBytes > 0L)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0L
    while (true) {
        val count = input.read(buffer)
        if (count == -1) break
        total += count
        if (total > maxBytes) throw FeedResponseTooLargeException(maxBytes)
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun notificationSampleSize(width: Int, height: Int, maxDimension: Int = 512): Int {
    var sampleSize = 1
    while (width / sampleSize > maxDimension * 2 || height / sampleSize > maxDimension * 2) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun Bitmap.fitNotificationSize(maxDimension: Int = 512): Bitmap {
    val largestDimension = maxOf(width, height)
    if (largestDimension <= maxDimension) return this
    val scale = maxDimension.toFloat() / largestDimension
    val scaled = this.scale(
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        filter = true,
    )
    if (scaled !== this) recycle()
    return scaled
}

private const val MaxNotificationImageBytes = 2L * 1024L * 1024L
