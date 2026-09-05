package com.shapeshed.booth

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import java.io.File
import androidx.core.net.toUri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.PlaybackSkipPolicy
import com.shapeshed.booth.data.orderedResumptionIds
import com.shapeshed.booth.data.ACTION_SLEEP_TIMER_CANCEL
import com.shapeshed.booth.data.ACTION_SLEEP_TIMER_SET
import com.shapeshed.booth.data.ACTION_SKIP_SILENCE_SET
import com.shapeshed.booth.data.SKIP_SILENCE_ENABLED
import com.shapeshed.booth.data.SLEEP_TIMER_DURATION_MS
import com.shapeshed.booth.data.SleepTimerState
import com.shapeshed.booth.data.SleepTimerStore
import com.shapeshed.booth.di.boothPlaybackEntryPoint
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

@UnstableApi
class PodcastPlaybackService : MediaLibraryService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession
    private lateinit var repository: PodcastRepository
    private var sleepTimerJob: Job? = null
    private var progressSaveJob: Job? = null
    private var currentMediaId: Long? = null
    private var introAppliedMediaId: Long? = null
    private var endingSkippedMediaId: Long? = null
    private var endHandledMediaId: Long? = null
    private val localFallbackAttempted = mutableSetOf<Long>()

    private companion object {
        const val TAG = "PodcastPlayback"
    }

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().also {
                it.setSmallIcon(R.drawable.ic_notification)
            },
        )
        repository = boothPlaybackEntryPoint(application).podcastRepository
        player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(30_000L)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(progressListener)
        progressSaveJob = serviceScope.launch {
            while (isActive) {
                delay(5_000L)
                maybeSkipEnding()
                saveCurrentProgress()
            }
        }
        val seekBackButton = CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
            .setDisplayName("Back 10 seconds")
            .setPlayerCommand(Player.COMMAND_SEEK_BACK)
            .setSlots(CommandButton.SLOT_BACK)
            .build()
        val seekForwardButton = CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
            .setDisplayName("Forward 30 seconds")
            .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
            .setSlots(CommandButton.SLOT_FORWARD)
            .build()
        session = MediaLibrarySession.Builder(this, player, SessionCallback(this))
            .setMediaButtonPreferences(listOf(seekBackButton, seekForwardButton))
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setBitmapLoader(CoilBitmapLoader(this))
            .build()
    }

    fun playEpisode(episodeId: Long) {
        serviceScope.launch {
            val episode = repository.episode(episodeId) ?: return@launch
            localFallbackAttempted.remove(episodeId)
            val item = mediaItem(episode)
            player.setMediaItem(item, episode.positionMs)
            player.prepare()
            player.play()
        }
    }

    private suspend fun mediaItem(episode: com.shapeshed.booth.data.EpisodeEntity): MediaItem {
        return mediaItem(episode, preferRemote = false)
    }

    private suspend fun mediaItem(
        episode: com.shapeshed.booth.data.EpisodeEntity,
        preferRemote: Boolean,
    ): MediaItem {
        val podcast = repository.podcast(episode.podcastId)
        val podcastTitle = podcast?.title
        val downloadedAudio = repository.downloadAsset(
            episode.id,
            com.shapeshed.booth.data.DownloadAssetType.AUDIO,
        )?.takeIf { it.status == com.shapeshed.booth.data.DownloadAssetStatus.COMPLETED }
            ?.destinationUri
        val audioUri = if (preferRemote) {
            episode.audioUrl
        } else {
            listOfNotNull(episode.localUri, downloadedAudio)
                .firstOrNull { File(it.toUri().path.orEmpty()).isFile }
                ?: episode.audioUrl
        }
        return MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(audioUri)
            .setMimeType(episode.mimeType)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setSubtitle(podcastTitle)
                    .setArtist(podcastTitle)
                    .setAlbumTitle(podcastTitle)
                    .setArtworkUri((episode.artworkUrl ?: podcast?.artworkUrl)?.let(android.net.Uri::parse))
                    .build(),
            )
            .build()
    }

    private val progressListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val nextId = mediaItem?.mediaId?.toLongOrNull()
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                currentMediaId?.let { completedId ->
                    serviceScope.launch { markQueueEpisodeCompleted(completedId) }
                }
            }
            currentMediaId = nextId
            endHandledMediaId = null
            introAppliedMediaId = null
            endingSkippedMediaId = null
            nextId?.let(::maybeSkipIntro)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && player.isPlaying) {
                currentMediaId?.let(::maybeSkipIntro)
            }
            if (playbackState == Player.STATE_ENDED) {
                val endedId = currentMediaId ?: player.currentMediaItem?.mediaId?.toLongOrNull()
                if (endedId != null && endHandledMediaId != endedId) {
                    endHandledMediaId = endedId
                    serviceScope.launch {
                        markQueueEpisodeCompleted(endedId)
                        if (player.hasNextMediaItem()) {
                            player.seekToNextMediaItem()
                            player.play()
                        } else {
                            playNextQueuedEpisode(endedId)
                        }
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                currentMediaId?.let(::maybeSkipIntro)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val episodeId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
            serviceScope.launch {
                val episode = repository.episode(episodeId) ?: return@launch
                val downloadedAudio = repository.downloadAsset(
                    episode.id,
                    com.shapeshed.booth.data.DownloadAssetType.AUDIO,
                )?.takeIf { it.status == com.shapeshed.booth.data.DownloadAssetStatus.COMPLETED }
                    ?.destinationUri
                val hasLocalAudio = listOfNotNull(episode.localUri, downloadedAudio)
                    .any { File(it.toUri().path.orEmpty()).isFile }
                if (hasLocalAudio && localFallbackAttempted.add(episodeId)) {
                    val wasPlaying = player.playWhenReady
                    val position = player.currentPosition.coerceAtLeast(0L)
                    player.setMediaItem(mediaItem(episode, preferRemote = true), position)
                    player.prepare()
                    if (wasPlaying) player.play()
                }
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (!events.containsAny(
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                )
            ) return
            if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                Log.d(TAG, "position discontinuity episode=${player.currentMediaItem?.mediaId} position=${player.currentPosition}")
                if (player.currentPosition <= 0L && currentMediaId != null) {
                    introAppliedMediaId = null
                    Log.d(TAG, "cleared intro skip guard after reset episode=$currentMediaId")
                }
            }
            serviceScope.launch {
                saveCurrentProgress()
            }
        }
    }

    private fun maybeSkipIntro(episodeId: Long) {
        if (introAppliedMediaId == episodeId) return
        serviceScope.launch {
            val episode = repository.episode(episodeId) ?: return@launch
            val skipMs = repository.podcast(episode.podcastId)?.skipStartSeconds.orZero() * 1_000L
            val duration = player.duration
            val position = player.currentPosition
            val target = PlaybackSkipPolicy.introPosition(position, skipMs, duration)
            Log.d(TAG, "intro check episode=$episodeId position=$position skipMs=$skipMs duration=$duration target=$target")
            if (target != position) {
                player.seekTo(target)
            }
            introAppliedMediaId = episodeId
        }
    }

    private suspend fun maybeSkipEnding() {
        val episodeId = currentMediaId ?: return
        if (endingSkippedMediaId == episodeId) return
        val duration = player.duration.takeIf { it > 0L } ?: return
        val episode = repository.episode(episodeId) ?: return
        val skipMs = repository.podcast(episode.podcastId)?.skipEndSeconds.orZero() * 1_000L
        if (PlaybackSkipPolicy.shouldSkipEnding(
                positionMs = player.currentPosition,
                durationMs = duration,
                skipEndMs = skipMs,
                playbackSpeed = player.playbackParameters.speed,
            )
        ) {
            endingSkippedMediaId = episodeId
            player.seekTo(duration)
        }
    }

    private suspend fun markQueueEpisodeCompleted(episodeId: Long) {
        repository.markCompletedAndRemoveFromQueue(episodeId)
    }

    private fun playNextQueuedEpisode(endedEpisodeId: Long) {
        serviceScope.launch {
            val nextEpisode = repository.queue
                .first()
                .sortedBy { it.position }
                .firstOrNull { it.episodeId != endedEpisodeId }
                ?.let { repository.episode(it.episodeId) }
                ?: return@launch
            val item = mediaItem(nextEpisode)
            player.setMediaItem(item, nextEpisode.positionMs)
            player.prepare()
            player.play()
        }
    }

    private suspend fun saveCurrentProgress() {
        if (!::player.isInitialized) return
        val id = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val durationMs = player.duration.takeIf { it > 0L }
        Log.d(TAG, "persist progress episode=$id position=${player.currentPosition} state=${player.playbackState}")
        repository.updateProgress(
            episodeId = id,
            positionMs = player.currentPosition,
            completed = player.playbackState == Player.STATE_ENDED,
            durationMs = durationMs,
        )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        runCatching {
            runBlocking(Dispatchers.IO) { saveCurrentProgress() }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = session

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        SleepTimerStore.set(null)
        runCatching {
            runBlocking(Dispatchers.IO) { saveCurrentProgress() }
        }
        if (::player.isInitialized) player.removeListener(progressListener)
        if (::session.isInitialized) session.release()
        if (::player.isInitialized) player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private class SessionCallback(private val service: PodcastPlaybackService) : MediaLibrarySession.Callback {
        private val sleepTimerSet = SessionCommand(ACTION_SLEEP_TIMER_SET, Bundle.EMPTY)
        private val sleepTimerCancel = SessionCommand(ACTION_SLEEP_TIMER_CANCEL, Bundle.EMPTY)
        private val skipSilenceSet = SessionCommand(ACTION_SKIP_SILENCE_SET, Bundle.EMPTY)

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(sleepTimerSet)
                .add(sleepTimerCancel)
                .add(skipSilenceSet)
                .build()
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                .buildUpon()
                .add(Player.COMMAND_SEEK_BACK)
                .add(Player.COMMAND_SEEK_FORWARD)
                .build()
            return MediaSession.ConnectionResult.accept(sessionCommands, playerCommands)
        }

        override fun onPlaybackResumption(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
            CallbackToFutureAdapter.getFuture { completer ->
                service.serviceScope.launch(Dispatchers.IO) {
                    runCatching {
                        val episodeId = boothPlaybackEntryPoint(service.application)
                            .settings.podcastLastEpisodeId.first()
                        val episode = episodeId?.let { service.repository.episode(it) }
                        if (episode == null) {
                            completer.set(
                                MediaSession.MediaItemsWithStartPosition(emptyList(), 0, C.TIME_UNSET),
                            )
                        } else {
                            val queuedEpisodes = service.repository.queue.first()
                                .mapNotNull { service.repository.episode(it.episodeId) }
                            val resumedEpisodeIds = orderedResumptionIds(
                                activeEpisodeId = episode.id,
                                queuedEpisodeIds = queuedEpisodes.map { it.id },
                            )
                            val episodesById = (queuedEpisodes + episode).associateBy { it.id }
                            val resumedEpisodes = resumedEpisodeIds.mapNotNull(episodesById::get)
                            completer.set(
                                MediaSession.MediaItemsWithStartPosition(
                                    resumedEpisodes.map { service.mediaItem(it) },
                                    resumedEpisodes.indexOfFirst { it.id == episode.id },
                                    episode.positionMs,
                                ),
                            )
                        }
                    }.onFailure(completer::setException)
                }
                "podcast-playback-resumption"
            }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_SLEEP_TIMER_SET -> {
                    val duration = args.getLong(SLEEP_TIMER_DURATION_MS).coerceAtLeast(1_000L)
                    service.startSleepTimer(duration)
                }
                ACTION_SLEEP_TIMER_CANCEL -> service.cancelSleepTimer()
                ACTION_SKIP_SILENCE_SET -> service.player.setSkipSilenceEnabled(args.getBoolean(SKIP_SILENCE_ENABLED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        sleepTimerJob = serviceScope.launch {
            var remainingMs = durationMs
            SleepTimerStore.set(SleepTimerState(durationMs, remainingMs))
            while (remainingMs > 0L) {
                val wasPlaying = player.isPlaying
                val tickStartedAt = android.os.SystemClock.elapsedRealtime()
                delay(250L)
                if (wasPlaying) {
                    remainingMs -= android.os.SystemClock.elapsedRealtime() - tickStartedAt
                }
                if (remainingMs > 0L) {
                    SleepTimerStore.set(SleepTimerState(durationMs, remainingMs))
                }
            }
            SleepTimerStore.set(null)
            player.pause()
        }
    }

    private fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        SleepTimerStore.set(null)
    }
}

private fun Int?.orZero(): Int = this ?: 0
