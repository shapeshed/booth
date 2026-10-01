package com.shapeshed.booth

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.shapeshed.booth.data.ACTION_SKIP_SILENCE_SET
import com.shapeshed.booth.data.ACTION_SLEEP_TIMER_CANCEL
import com.shapeshed.booth.data.ACTION_SLEEP_TIMER_SET
import com.shapeshed.booth.data.PlaybackSkipPolicy
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.SKIP_SILENCE_ENABLED
import com.shapeshed.booth.data.SLEEP_TIMER_DURATION_MS
import com.shapeshed.booth.data.SettingsStore
import com.shapeshed.booth.data.SleepTimerBudget
import com.shapeshed.booth.data.SleepTimerState
import com.shapeshed.booth.data.SleepTimerStore
import com.shapeshed.booth.data.orderedResumptionIds
import com.shapeshed.booth.di.boothPlaybackEntryPoint
import java.io.File
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs [compute] in [scope] and completes the returned future with its result.
 *
 * The client waiting on this is outside the app, such as Android Auto or the assistant, so the one
 * thing that must not happen is a future left pending: `CallbackToFutureAdapter.Completer` has no
 * `cancel()`, so there is nothing to time it out and the client would hang indefinitely.
 *
 * Two things guarantee the future always resolves:
 *
 * - `invokeOnCompletion` fires even for a coroutine that is cancelled before its body starts, which
 *   is the window a plain `launch` leaves open. `set` and `setException` both no-op once the future
 *   is resolved, so racing them is safe.
 * - the `try`/`catch` covers the work itself once it is running.
 *
 * Cancellation is reported as the [CancellationException] it is, rather than being swallowed by
 * `runCatching` and turned into a resumption failure. A service shutting down is not an error the
 * client should be told about.
 *
 * Cancelling the future cancels the work, so a client that walks away does not leave the database
 * being queried on its behalf.
 */
internal fun <T> resumptionFuture(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    compute: suspend () -> T,
): ListenableFuture<T> = CallbackToFutureAdapter.getFuture { completer ->
    val job = scope.launch(dispatcher) {
        try {
            completer.set(compute())
        } catch (error: Exception) {
            completer.setException(error)
        }
    }
    job.invokeOnCompletion { cause ->
        if (cause != null) {
            completer.setException(cause)
        }
    }
    completer.addCancellationListener({ job.cancel() }, Executor { it.run() })
    "podcast-playback-resumption"
}

@UnstableApi
class PodcastPlaybackService : MediaLibraryService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession
    private lateinit var repository: PodcastRepository
    private var settings: SettingsStore? = null
    private var sleepTimerJob: Job? = null
    private var progressSaveJob: Job? = null
    private var currentMediaId: Long? = null
    private var introAppliedMediaId: Long? = null
    private var endingSkippedMediaId: Long? = null
    private var endHandledMediaId: Long? = null
    private val localFallbackAttempted = mutableSetOf<Long>()

    private companion object {
        const val TAG = "PodcastPlayback"

        /** How often the player position is flushed to Room while the service lives. */
        const val PROGRESS_SAVE_INTERVAL_MS = 5_000L

        /** Ceiling for the one blocking write in onDestroy, so a locked database cannot ANR. */
        const val FINAL_PROGRESS_SAVE_TIMEOUT_MS = 250L

        /** How often the sleep timer samples playback progress. */
        const val SLEEP_TIMER_TICK_MS = 250L

        /**
         * How often the sleep timer checkpoints itself to settings. Bounds how much of the budget a
         * process death can lose.
         */
        const val SLEEP_TIMER_CHECKPOINT_MS = 10_000L
    }

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().also {
                it.setSmallIcon(R.drawable.ic_notification)
            },
        )
        val entryPoint = boothPlaybackEntryPoint(application)
        repository = entryPoint.podcastRepository
        settings = entryPoint.settings
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
        restoreSleepTimer()
        progressSaveJob = serviceScope.launch {
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
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
        // Car stereos commonly send the generic media "next" command for their physical
        // next-track button. For spoken-word playback that should behave like the in-app
        // forward button so listeners can skip adverts without leaving the episode.
        val sessionPlayer = object : ForwardingPlayer(player) {
            override fun seekToPrevious() = seekBack()
            override fun seekToNext() = seekForward()
        }
        session = MediaLibrarySession.Builder(this, sessionPlayer, SessionCallback(this))
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

    private suspend fun mediaItem(episode: com.shapeshed.booth.data.EpisodeEntity): MediaItem =
        mediaItem(episode, preferRemote = false)

    private suspend fun mediaItem(episode: com.shapeshed.booth.data.EpisodeEntity, preferRemote: Boolean): MediaItem {
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
            ) {
                return
            }
            if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                Log.d(
                    TAG,
                    "position discontinuity episode=${player.currentMediaItem?.mediaId} position=${player.currentPosition}",
                )
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
            Log.d(
                TAG,
                "intro check episode=$episodeId position=$position skipMs=$skipMs duration=$duration target=$target",
            )
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
        com.shapeshed.booth.data.PlayedDownloadCleanupWorker.schedule(applicationContext, episodeId)
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
        // No write here. progressSaveJob already persists every PROGRESS_SAVE_INTERVAL_MS, so
        // the worst case here is that much staleness. A blocking Room write on the main thread
        // would contend with the refresh worker's SQLite lock at exactly the moment the platform
        // is tearing the task down, which is the worst possible time to risk an ANR.
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = session

    override fun onDestroy() {
        cancelSleepTimerForTeardown()
        // Bounded so a contended SQLite lock cannot stall the main thread indefinitely. Losing
        // this final write costs at most PROGRESS_SAVE_INTERVAL_MS of playback position, which
        // is a much better trade than an ANR during service teardown.
        runCatching {
            runBlocking { withTimeoutOrNull(FINAL_PROGRESS_SAVE_TIMEOUT_MS) { saveCurrentProgress() } }
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

        /**
         * Many Bluetooth headsets expose a next-track key rather than a dedicated fast-forward
         * key. For spoken-word playback, treat that key as the same 30-second seek instead of
         * depending on a next media item being available in the player playlist.
         */
        override fun onMediaButtonEvent(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            intent: Intent,
        ): Boolean {
            val event = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                ?: return false
            if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0 ||
                event.keyCode != KeyEvent.KEYCODE_MEDIA_NEXT
            ) {
                return false
            }
            session.player.seekForward()
            return true
        }

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

        /**
         * Answers an external controller's request to resume playback, such as Android Auto or the
         * assistant.
         *
         * The lifetime rules live in [resumptionFuture]; this override is only the wiring, so that
         * the part that can hang a client is testable on its own.
         */
        override fun onPlaybackResumption(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = resumptionFuture(service.serviceScope) {
            service.resumptionItems()
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

    /**
     * The queue an external controller should resume: the last-played episode first, then whatever
     * was queued, de-duplicated. Empty when nothing has been played yet.
     */
    private suspend fun resumptionItems(): MediaSession.MediaItemsWithStartPosition {
        val episodeId = settings?.podcastLastEpisodeId?.first()
        val episode = episodeId?.let { repository.episode(it) }
            ?: return MediaSession.MediaItemsWithStartPosition(emptyList(), 0, C.TIME_UNSET)
        val queuedEpisodes = repository.queue.first()
            .mapNotNull { repository.episode(it.episodeId) }
        val resumedEpisodeIds = orderedResumptionIds(
            activeEpisodeId = episode.id,
            queuedEpisodeIds = queuedEpisodes.map { it.id },
        )
        val episodesById = (queuedEpisodes + episode).associateBy { it.id }
        val resumedEpisodes = resumedEpisodeIds.mapNotNull(episodesById::get)
        return MediaSession.MediaItemsWithStartPosition(
            resumedEpisodes.map { mediaItem(it) },
            resumedEpisodes.indexOfFirst { it.id == episode.id },
            episode.positionMs,
        )
    }

    private fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        sleepTimerJob = serviceScope.launch {
            val budget = SleepTimerBudget(durationMs)
            publishSleepTimer(budget, persist = true)
            var sinceCheckpointMs = 0L
            while (isActive && !budget.isExhausted) {
                if (!player.isPlaying) {
                    // A sleep timer measures playback time, so paused time does not count. Wait for
                    // playback to resume instead of polling: the previous loop woke four times a
                    // second for as long as the user left it paused, and never returned.
                    awaitPlaying()
                    continue
                }
                val startedAt = android.os.SystemClock.elapsedRealtime()
                delay(SLEEP_TIMER_TICK_MS)
                val elapsed = android.os.SystemClock.elapsedRealtime() - startedAt
                sinceCheckpointMs += elapsed
                val finished = budget.consume(elapsed)
                if (sinceCheckpointMs >= SLEEP_TIMER_CHECKPOINT_MS) {
                    sinceCheckpointMs = 0L
                    publishSleepTimer(budget, persist = true)
                } else {
                    publishSleepTimer(budget, persist = false)
                }
                if (finished) break
            }
            finishSleepTimer()
        }
    }

    private suspend fun awaitPlaying() {
        if (player.isPlaying) return
        player.playingState().first { it }
    }

    private fun publishSleepTimer(budget: SleepTimerBudget, persist: Boolean) {
        val state = budget.state()
        SleepTimerStore.set(state)
        if (persist) {
            serviceScope.launch { settings?.setSleepTimer(state) }
        }
    }

    private fun finishSleepTimer() {
        sleepTimerJob = null
        SleepTimerStore.clear()
        serviceScope.launch { settings?.clearSleepTimer() }
        player.pause()
    }

    private fun cancelSleepTimer() {
        stopSleepTimer(clearPersisted = true)
    }

    /** Teardown path: stop the in-memory timer but keep the persisted budget for a restart. */
    private fun cancelSleepTimerForTeardown() {
        stopSleepTimer(clearPersisted = false)
    }

    /**
     * Stops the in-memory timer.
     *
     * [clearPersisted] is false when the service is being torn down. The persisted budget is what
     * [restoreSleepTimer] reads back, so clearing it here would discard the timer on every service
     * restart, which is exactly the case it exists to survive. It is cleared when the timer
     * actually expires or when the user cancels it.
     */
    private fun stopSleepTimer(clearPersisted: Boolean) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        SleepTimerStore.clear()
        if (clearPersisted) {
            serviceScope.launch { settings?.clearSleepTimer() }
        }
    }

    /**
     * Resumes a timer that was set before the process died.
     *
     * The persisted value is remaining *playback* time, not a wall-clock deadline, so a timer set
     * for 30 minutes resumes with the time that was left rather than expiring while the app was not
     * running.
     */
    private fun restoreSleepTimer() {
        serviceScope.launch {
            val persisted = settings?.sleepTimer?.first() ?: return@launch
            if (persisted.remainingMs <= 0L) {
                settings?.clearSleepTimer()
                return@launch
            }
            startSleepTimer(persisted.remainingMs)
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0

/**
 * Bridges [Player.isPlaying] to a Flow.
 *
 * The sleep timer needs to suspend until playback resumes rather than poll for it, and polling
 * four times a second for as long as the user leaves the app paused is a busy loop that never
 * returns. The listener is removed in [awaitClose], so cancelling the collecting coroutine,
 * including when the service tears the timer down, does not leak it.
 */
private fun Player.playingState(): Flow<Boolean> = callbackFlow {
    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            trySend(isPlaying)
        }
    }
    addListener(listener)
    trySend(isPlaying)
    awaitClose { removeListener(listener) }
}.distinctUntilChanged()
