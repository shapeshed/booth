package com.shapeshed.booth.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shapeshed.booth.BuildConfig
import java.util.Locale
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.concurrent.TimeUnit

/** Portable, schema-independent export of subscriptions and listening state. */
class PodcastBackupManager(
    private val repository: PodcastRepository,
    private val settings: SettingsStore,
    private val context: Context? = null,
    private val restoreDownload: suspend (EpisodeEntity) -> Unit = { episode ->
        context?.let { restoreContext ->
            val network = settings.podcastDownloadNetwork.first()
            val request = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
                .setInputData(workDataOf(EPISODE_ID_INPUT to episode.id))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(
                            if (network == PodcastDownloadNetwork.WIFI_ONLY) {
                                NetworkType.UNMETERED
                            } else {
                                NetworkType.CONNECTED
                            },
                        )
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(restoreContext.applicationContext).enqueueUniqueWork(
                "podcast-download-${episode.id}",
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    },
) {
    suspend fun export(): String {
        val subscriptions = repository.podcasts.first()
        val podcastsById = subscriptions.associateBy(PodcastEntity::id)
        val episodes = repository.allEpisodesSnapshot()
        val downloadedEpisodeIds = repository.downloadAssets.first()
            .filter {
                it.assetType == DownloadAssetType.AUDIO &&
                    it.status in setOf(DownloadAssetStatus.QUEUED, DownloadAssetStatus.DOWNLOADING, DownloadAssetStatus.RETRYING, DownloadAssetStatus.COMPLETED)
            }
            .mapTo(HashSet(), DownloadAssetEntity::episodeId)
        val episodeIndex = JSONArray()
        episodes.asSequence()
            .filter { it.podcastId in podcastsById }
            .forEach { episode ->
                val podcast = podcastsById.getValue(episode.podcastId)
                episodeIndex.put(
                    JSONObject()
                        .put("feedUrl", podcast.feedUrl)
                        .put("guid", episode.guid)
                        .put("title", episode.title)
                        .put("audioUrl", episode.audioUrl)
                        .put("mimeType", episode.mimeType ?: JSONObject.NULL)
                        .put("artworkUrl", episode.artworkUrl ?: JSONObject.NULL)
                        .put("publishedAtMillis", episode.publishedAtMillis ?: JSONObject.NULL)
                        .put("durationMs", episode.durationMs ?: JSONObject.NULL)
                        .put("inInbox", episode.inInbox)
                        .put("positionMs", episode.positionMs)
                        .put("completed", episode.completed)
                        .put("favorite", episode.favorite)
                        .put("downloaded", episode.id in downloadedEpisodeIds),
                )
            }
        val playback = JSONArray()
        val queueJson = JSONArray()
        repository.queue.first().forEach { queueItem ->
            episodes.firstOrNull { it.id == queueItem.episodeId }?.let { episode ->
                podcastsById[episode.podcastId]?.let { podcast ->
                    queueJson.put(JSONObject().put("feedUrl", podcast.feedUrl).put("guid", episode.guid).put("audioUrl", episode.audioUrl).put("position", queueItem.position))
                }
            }
        }
        episodes
            .asSequence()
            .filter { it.podcastId in podcastsById }
            .filter { it.positionMs > 0L || it.completed || it.favorite }
            .forEach { episode ->
                val podcast = podcastsById.getValue(episode.podcastId)
                playback.put(
                    JSONObject()
                        .put("feedUrl", podcast.feedUrl)
                        .put("guid", episode.guid)
                        .put("title", episode.title)
                        .put("audioUrl", episode.audioUrl)
                        .put("positionMs", episode.positionMs)
                        .put("durationMs", episode.durationMs ?: JSONObject.NULL)
                        .put("completed", episode.completed)
                        .put("favorite", episode.favorite),
                )
            }

        val subscriptionJson = JSONArray()
        subscriptions.forEach { podcast ->
            subscriptionJson.put(
                JSONObject()
                    .put("feedUrl", podcast.feedUrl)
                    .put("title", podcast.title)
                    .put("author", podcast.author ?: JSONObject.NULL)
                    .put("descriptionHtml", podcast.descriptionHtml ?: JSONObject.NULL)
                    .put("siteUrl", podcast.siteUrl ?: JSONObject.NULL)
                    .put("artworkUrl", podcast.artworkUrl ?: JSONObject.NULL)
                    .put("tags", podcast.tags)
                    .put("skipStartSeconds", podcast.skipStartSeconds)
                    .put("skipEndSeconds", podcast.skipEndSeconds)
                    .put("playbackSpeed", podcast.playbackSpeed ?: JSONObject.NULL)
                    .put("skipSilence", podcast.skipSilence ?: JSONObject.NULL)
                    .put("includeInAutoRefresh", podcast.includeInAutoRefresh)
                    .put("includeInAutoDownload", podcast.includeInAutoDownload)
                    .put("includeInVideoDownload", podcast.includeInVideoDownload)
                    .put("includeInNotifications", podcast.includeInNotifications)
                    .put("includeInAutoQueue", podcast.includeInAutoQueue)
                    .put("lastRefreshMillis", podcast.lastRefreshMillis ?: JSONObject.NULL)
                    .put("feedEtag", podcast.feedEtag ?: JSONObject.NULL)
                    .put("feedLastModified", podcast.feedLastModified ?: JSONObject.NULL),
            )
        }

        return JSONObject()
            .put("format", FORMAT)
            .put("formatVersion", FORMAT_VERSION)
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("exportedAtMillis", System.currentTimeMillis())
            .put(
                "playbackSettings",
                JSONObject()
                    .put("playbackSpeed", settings.podcastPlaybackSpeed.first().toDouble())
                    .put("skipSilence", settings.podcastSkipSilence.first()),
            )
            .put(
                "globalSettings",
                JSONObject()
                    .put("subscriptionsViewMode", settings.podcastSubscriptionsViewMode.first().name)
                    .put("selectedTab", settings.podcastSelectedTab.first() ?: JSONObject.NULL)
                    .put("refreshInterval", settings.podcastRefreshInterval.first().name)
                    .put("refreshNetwork", settings.podcastRefreshNetwork.first().name)
                    .put("notificationsEnabled", settings.podcastNotificationsEnabled.first())
                    .put("autoQueueEnabled", settings.podcastAutoQueueEnabled.first())
                    .put("downloadEpisodesAddedToUpNext", settings.podcastDownloadEpisodesAddedToUpNext.first())
                    .put("downloadNetwork", settings.podcastDownloadNetwork.first().name)
                    .put("downloadLimit", settings.podcastDownloadLimit.first().name)
                    .put("deleteBeforeAutoDownload", settings.podcastDeleteBeforeAutoDownload.first().name)
                    .put("removePlayedDownloads", settings.podcastRemovePlayedDownloads.first())
                    .put("downloadVideos", settings.podcastDownloadVideos.first())
                    .put("searchProvider", settings.podcastSearchProvider.first()),
            )
            .put("subscriptions", subscriptionJson)
            .put("episodes", episodeIndex)
            .put("queue", queueJson)
            .put("playback", playback)
            .toString(2)
    }

    suspend fun exportZip(): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(export().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }.toByteArray()

    suspend fun importZip(bytes: ByteArray): Int {
        val json = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }
                .firstOrNull { it.name == "backup.json" }
                ?.let { zip.readBytes().toString(Charsets.UTF_8) }
                ?: error("Backup ZIP does not contain backup.json")
        }
        return import(json)
    }

    /** Merges a portable backup without deleting current subscriptions, episodes, or downloads. */
    suspend fun import(json: String): Int {
        val document = JSONObject(json)
        require(isSupported(document)) { "Unsupported Booth backup format" }
        val subscriptions = document.optJSONArray("subscriptions") ?: JSONArray()
        val hasEpisodeIndex = document.has("episodes")
        val importedIds = mutableMapOf<String, Long>()
        for (index in 0 until subscriptions.length()) {
            val item = subscriptions.optJSONObject(index) ?: continue
            val feedUrl = item.optString("feedUrl").trim().takeIf { it.isNotBlank() } ?: continue
            val existing = repository.podcastByFeedUrl(feedUrl)
            val id = existing?.id ?: podcastId(feedUrl)
            repository.upsertBackupPodcast(
                PodcastEntity(
                    id = id,
                    title = item.optString("title", existing?.title.orEmpty()),
                    author = item.optionalString("author") ?: existing?.author,
                    feedUrl = feedUrl,
                    siteUrl = item.optionalString("siteUrl") ?: existing?.siteUrl,
                    descriptionHtml = item.optionalString("descriptionHtml") ?: existing?.descriptionHtml,
                    artworkUrl = item.optionalString("artworkUrl") ?: existing?.artworkUrl,
                    explicit = existing?.explicit,
                    tags = item.optString("tags", existing?.tags.orEmpty()),
                    skipStartSeconds = item.optInt("skipStartSeconds", existing?.skipStartSeconds ?: 0),
                    skipEndSeconds = item.optInt("skipEndSeconds", existing?.skipEndSeconds ?: 0),
                    playbackSpeed = item.optionalDouble("playbackSpeed")?.toFloat() ?: existing?.playbackSpeed,
                    skipSilence = item.optionalBoolean("skipSilence") ?: existing?.skipSilence,
                    includeInAutoRefresh = item.optBoolean("includeInAutoRefresh", existing?.includeInAutoRefresh ?: true),
                    // Backups created before the episode index was added cannot tell which feed
                    // items were already known. Keep those subscriptions safe from downloading
                    // the entire historical feed until the user explicitly enables this again.
                    includeInAutoDownload = item.optBoolean("includeInAutoDownload", existing?.includeInAutoDownload ?: false) &&
                        (hasEpisodeIndex || existing != null),
                    includeInVideoDownload = item.optBoolean("includeInVideoDownload", existing?.includeInVideoDownload ?: true),
                    includeInNotifications = item.optBoolean("includeInNotifications", existing?.includeInNotifications ?: false),
                    includeInAutoQueue = item.optBoolean("includeInAutoQueue", existing?.includeInAutoQueue ?: false),
                    isSubscribed = true,
                    subscribedAtMillis = existing?.subscribedAtMillis ?: System.currentTimeMillis(),
                    lastRefreshMillis = item.optLongOrNull("lastRefreshMillis") ?: existing?.lastRefreshMillis,
                    feedEtag = item.optionalString("feedEtag") ?: existing?.feedEtag,
                    feedLastModified = item.optionalString("feedLastModified") ?: existing?.feedLastModified,
                    lastRefreshAttemptMillis = existing?.lastRefreshAttemptMillis,
                    lastRefreshError = existing?.lastRefreshError,
                ),
            )
            importedIds[feedUrl] = id
        }

        // Restore episode identities before playback so the next refresh only treats genuinely
        // new feed items as new (and does not auto-download an entire historical feed).
        val importedEpisodes = document.optJSONArray("episodes") ?: JSONArray()
        for (index in 0 until importedEpisodes.length()) {
            val item = importedEpisodes.optJSONObject(index) ?: continue
            val feedUrl = item.optString("feedUrl").trim()
            val podcastId = importedIds[feedUrl] ?: continue
            val guid = item.optString("guid").trim().takeIf { it.isNotBlank() } ?: continue
            val audioUrl = item.optString("audioUrl").trim().takeIf { it.isNotBlank() } ?: continue
            val existing = repository.episodes(podcastId).first().firstOrNull {
                it.guid == guid || it.audioUrl == audioUrl
            }
            if (existing == null) {
                repository.upsertBackupEpisode(
                    EpisodeEntity(
                        id = episodeId(podcastId, guid, audioUrl),
                        podcastId = podcastId,
                        guid = guid,
                        title = item.optString("title", guid),
                        descriptionHtml = null,
                        audioUrl = audioUrl,
                        mimeType = item.optionalString("mimeType"),
                        artworkUrl = item.optionalString("artworkUrl"),
                        publishedAtMillis = item.optLongOrNull("publishedAtMillis"),
                        durationMs = item.optLongOrNull("durationMs"),
                        positionMs = 0L,
                        completed = item.optBoolean("completed", false),
                        localUri = null,
                        inInbox = item.optBoolean("inInbox", false),
                        firstSeenAtMillis = null,
                    ),
                )
            }
            val restored = repository.episodes(podcastId).first().firstOrNull { it.guid == guid || it.audioUrl == audioUrl }
            if (restored != null && (item.optLong("positionMs", 0L) > 0L || item.optBoolean("completed", false) || item.optBoolean("favorite", false))) {
                repository.restoreBackupPlayback(
                    restored.id,
                    item.optLong("positionMs", 0L).coerceAtLeast(0L),
                    item.optBoolean("completed", false),
                    item.optLongOrNull("durationMs"),
                    item.optBoolean("favorite", false),
                )
            }
            restored?.let { repository.restoreBackupInboxState(it.id, item.optBoolean("inInbox", false)) }
        }

        // Downloaded media files cannot be copied between app sandboxes. Collect the intent now;
        // requests are constrained and capped after the backed-up settings are restored below.
        val downloadCandidates = mutableListOf<EpisodeEntity>()
        for (index in 0 until importedEpisodes.length()) {
            val item = importedEpisodes.optJSONObject(index) ?: continue
            if (!item.optBoolean("downloaded", false)) continue
            val podcastId = importedIds[item.optString("feedUrl").trim()] ?: continue
            val guid = item.optString("guid").trim()
            val audioUrl = item.optString("audioUrl").trim()
            val episode = repository.episodes(podcastId).first().firstOrNull { it.guid == guid || it.audioUrl == audioUrl }
                ?: continue
            downloadCandidates += episode
        }

        val playback = document.optJSONArray("playback") ?: JSONArray()
        for (index in 0 until playback.length()) {
            val item = playback.optJSONObject(index) ?: continue
            val feedUrl = item.optString("feedUrl").trim()
            val podcastId = importedIds[feedUrl] ?: repository.podcastByFeedUrl(feedUrl)?.id ?: continue
            val guid = item.optString("guid").trim().takeIf { it.isNotBlank() } ?: continue
            val audioUrl = item.optString("audioUrl").trim().takeIf { it.isNotBlank() } ?: continue
            val existing = repository.episodes(podcastId).first().firstOrNull {
                it.guid == guid || it.audioUrl == audioUrl
            }
            val episodeId = existing?.id ?: episodeId(podcastId, guid, audioUrl)
            if (existing == null) {
                repository.upsertBackupEpisode(
                    EpisodeEntity(
                        id = episodeId,
                        podcastId = podcastId,
                        guid = guid,
                        title = item.optString("title", guid),
                        descriptionHtml = null,
                        audioUrl = audioUrl,
                        mimeType = null,
                        artworkUrl = null,
                        publishedAtMillis = null,
                        durationMs = item.optLongOrNull("durationMs"),
                        positionMs = 0L,
                        completed = false,
                        localUri = null,
                        inInbox = false,
                        firstSeenAtMillis = null,
                    ),
                )
            }
            repository.restoreBackupPlayback(
                episodeId = episodeId,
                positionMs = item.optLong("positionMs", 0L).coerceAtLeast(0L),
                completed = item.optBoolean("completed", false),
                durationMs = item.optLongOrNull("durationMs"),
                favorite = item.optBoolean("favorite", false),
            )
        }

        val restoredQueue = mutableListOf<Long>()
        val queue = document.optJSONArray("queue") ?: JSONArray()
        for (index in 0 until queue.length()) {
            val item = queue.optJSONObject(index) ?: continue
            val podcastId = importedIds[item.optString("feedUrl").trim()] ?: continue
            val guid = item.optString("guid").trim()
            val audioUrl = item.optString("audioUrl").trim()
            repository.episodes(podcastId).first().firstOrNull { it.guid == guid || it.audioUrl == audioUrl }?.let {
                restoredQueue += it.id
            }
        }
        if (restoredQueue.isNotEmpty()) repository.reorderQueue(restoredQueue)

        document.optJSONObject("playbackSettings")?.let { playbackSettings ->
            playbackSettings.optDoubleOrNull("playbackSpeed")?.toFloat()?.let { settings.setPodcastPlaybackSpeed(it) }
            playbackSettings.optBooleanOrNull("skipSilence")?.let { settings.setPodcastSkipSilence(it) }
        }
        document.optJSONObject("globalSettings")?.let { global ->
            global.optionalString("subscriptionsViewMode")?.toEnum<PodcastSubscriptionsViewMode>()?.let { settings.setPodcastSubscriptionsViewMode(it) }
            global.optionalString("selectedTab")?.let { settings.setPodcastSelectedTab(it) }
            global.optionalString("refreshInterval")?.toEnum<PodcastRefreshInterval>()?.let { settings.setPodcastRefreshInterval(it) }
            global.optionalString("refreshNetwork")?.toEnum<PodcastRefreshNetwork>()?.let { settings.setPodcastRefreshNetwork(it) }
            global.optionalBoolean("notificationsEnabled")?.let { settings.setPodcastNotificationsEnabled(it) }
            global.optionalBoolean("autoQueueEnabled")?.let { settings.setPodcastAutoQueueEnabled(it) }
            global.optionalBoolean("downloadEpisodesAddedToUpNext")?.let {
                settings.setPodcastDownloadEpisodesAddedToUpNext(it)
            }
            global.optionalString("downloadNetwork")?.toEnum<PodcastDownloadNetwork>()?.let { settings.setPodcastDownloadNetwork(it) }
            global.optionalString("downloadLimit")?.toEnum<PodcastDownloadLimit>()?.let { settings.setPodcastDownloadLimit(it) }
            global.optionalString("deleteBeforeAutoDownload")?.toEnum<PodcastDeleteBeforeAutoDownload>()?.let { settings.setPodcastDeleteBeforeAutoDownload(it) }
            global.optionalBoolean("removePlayedDownloads")?.let { settings.setPodcastRemovePlayedDownloads(it) }
            global.optionalBoolean("downloadVideos")?.let { settings.setPodcastDownloadVideos(it) }
            global.optionalString("searchProvider")?.let { settings.setPodcastSearchProvider(it) }
        }
        val allEpisodes = repository.podcasts.first()
            .flatMap { podcast -> repository.episodes(podcast.id).first() }
        val downloadsToRestore = context?.let { restoreContext ->
            PodcastDownloadManager(restoreContext, repository).downloadsWithinLimit(
                candidates = downloadCandidates,
                downloadedEpisodes = allEpisodes,
                downloadAssets = repository.downloadAssets.first(),
                queuedEpisodeIds = repository.queue.first().mapTo(mutableSetOf(), QueueEntity::episodeId),
                maximumDownloads = settings.podcastDownloadLimit.first().episodeCount,
                mode = settings.podcastDeleteBeforeAutoDownload.first(),
            )
        } ?: downloadCandidates
        downloadsToRestore.forEach { episode ->
            try {
                restoreDownload(episode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A failed media request must not prevent the remaining backup from restoring.
            }
        }
        return importedIds.size
    }

    companion object {
        const val FORMAT = "booth-backup"
        const val FORMAT_VERSION = 1

        fun isSupported(json: JSONObject): Boolean =
            isSupported(json.optString("format"), json.optInt("formatVersion", -1))

        fun isSupported(format: String, version: Int): Boolean =
            format.lowercase(Locale.ROOT) == FORMAT && version in 1..FORMAT_VERSION
    }
}

private fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
private fun JSONObject.optionalDouble(key: String): Double? = if (isNull(key) || !has(key)) null else optDouble(key)
private fun JSONObject.optionalBoolean(key: String): Boolean? = if (isNull(key) || !has(key)) null else optBoolean(key)
private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else optLong(key)
private fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key) || !has(key)) null else optDouble(key)
private fun JSONObject.optBooleanOrNull(key: String): Boolean? = if (isNull(key) || !has(key)) null else optBoolean(key)
private inline fun <reified T : Enum<T>> String.toEnum(): T? = runCatching { enumValueOf<T>(this) }.getOrNull()
