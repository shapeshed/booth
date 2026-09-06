package com.shapeshed.booth.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.shapeshed.booth.PodcastPlaybackService
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.DownloadAssetStatus
import com.shapeshed.booth.data.ACTION_SLEEP_TIMER_CANCEL
import com.shapeshed.booth.data.ACTION_SLEEP_TIMER_SET
import com.shapeshed.booth.data.ACTION_SKIP_SILENCE_SET
import com.shapeshed.booth.data.SKIP_SILENCE_ENABLED
import com.shapeshed.booth.data.SLEEP_TIMER_DURATION_MS
import com.shapeshed.booth.data.SleepTimerState
import com.shapeshed.booth.data.SleepTimerStore
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val episode: EpisodeEntity? = null,
    val isPlaying: Boolean = false,
    val speed: Float = 1f,
    val skipSilence: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isVideoMode: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackError: PodcastUiError? = null,
)

@SuppressLint("UnsafeOptInUsageError")
@HiltViewModel
class PodcastPlaybackViewModel @Inject constructor(
    injectedSettings: com.shapeshed.booth.data.SettingsStore,
    injectedRepository: PodcastRepository,
) : ViewModel() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var controllerListener: Player.Listener? = null
    private var settings: com.shapeshed.booth.data.SettingsStore? = injectedSettings
    private var repository: PodcastRepository? = injectedRepository
    private var preferredSpeed = 1f
    private var playlistEpisodes: Map<Long, EpisodeEntity> = emptyMap()
    private var playlistPodcastTitles: Map<Long, String> = emptyMap()
    private var videoMode = false
    // The user's foreground choice. Minimizing temporarily uses audio without changing this.
    private var foregroundVideoPreference = false
    // HLS carries audio and video on one prepared source. Audio mode can therefore hide the
    // surface without replacing the MediaItem or seeking through a rebuffering cycle.
    private var activeSourceIsVideo = false
    private var pendingNotificationPlay: Pair<EpisodeEntity, String>? = null
    private var displayedPositionMs = 0L
    private var displayedDurationMs = 0L
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    val player: Player?
        get() = controller

    val sleepTimer: StateFlow<SleepTimerState?> = SleepTimerStore.state

    fun connect(context: Context) {
        if (controllerFuture != null) return
        val appContext = context.applicationContext
        controllerFuture = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PodcastPlaybackService::class.java)),
        ).buildAsync()
        controllerFuture?.addListener({
            controller = runCatching { controllerFuture?.get() }.getOrNull()
            pendingNotificationPlay?.let { (episode, title) ->
                pendingNotificationPlay = null
                play(episode, title)
            }
            _isPlaying.value = controller?.isPlaying == true
            _state.value = _state.value.copy(isPlaying = controller?.isPlaying == true)
            viewModelScope.launch {
                preferredSpeed = settings?.podcastPlaybackSpeed?.first() ?: preferredSpeed
                val skipSilence = settings?.podcastSkipSilence?.first() ?: false
                controller?.setPlaybackSpeed(preferredSpeed)
                sendSkipSilenceCommand(skipSilence)
                _state.value = _state.value.copy(speed = preferredSpeed, skipSilence = skipSilence)
                if (controller?.currentMediaItem == null) {
                    settings?.podcastLastEpisodeId?.first()
                        ?.let { repository?.episode(it) }
                        ?.let(::restoreLastEpisode)
                } else {
                    syncCurrentEpisode()
                }
                while (isActive) {
                    controller?.let { mediaController ->
                        val rawPositionMs = mediaController.currentPosition.coerceAtLeast(0L)
                        val rawDurationMs = mediaController.duration.takeIf { it > 0L }
                        // Preparing a restored, paused item briefly reports BUFFERING. It is
                        // only user-visible buffering when playback is actually intended.
                        val bufferingState = mediaController.playbackState == Player.STATE_BUFFERING &&
                            mediaController.playWhenReady
                        val preparing = mediaController.playbackState == Player.STATE_IDLE ||
                            mediaController.playbackState == Player.STATE_BUFFERING
                        // Keep the initial streaming state until Media3 is ready. Debouncing this
                        // transition briefly exposed Play/equalizer between two buffering ticks.
                        val buffering = bufferingState ||
                            (_state.value.isBuffering && preparing && mediaController.playWhenReady)
                        if (!preparing || rawPositionMs >= displayedPositionMs - 1_000L) {
                            displayedPositionMs = rawPositionMs
                        }
                        rawDurationMs?.let { displayedDurationMs = it }
                        _state.value = _state.value.copy(
                            positionMs = if (preparing) displayedPositionMs else rawPositionMs,
                            durationMs = rawDurationMs ?: displayedDurationMs,
                            isBuffering = buffering,
                        )
                    }
                    delay(500L)
                }
            }
            controllerListener = object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    _state.value = _state.value.copy(
                        isPlaying = isPlaying,
                        playbackError = if (isPlaying) null else _state.value.playbackError,
                    )
                }

                override fun onPlayerError(error: PlaybackException) {
                    _isPlaying.value = false
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        playbackError = error.userMessage(),
                    )
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val episodeId = mediaItem?.mediaId?.toLongOrNull() ?: return
                    viewModelScope.launch {
                        val episode = playlistEpisodes[episodeId] ?: repository?.episode(episodeId)
                            ?: return@launch
                        playlistEpisodes = playlistEpisodes + (episode.id to episode)
                        val effectiveSpeed = repository?.podcast(episode.podcastId)?.playbackSpeed ?: preferredSpeed
                        controller?.setPlaybackSpeed(effectiveSpeed)
                        _state.value = _state.value.copy(
                            episode = episode,
                            speed = effectiveSpeed,
                            isPlaying = controller?.isPlaying == true,
                            positionMs = displayedPositionMs,
                            durationMs = displayedDurationMs.takeIf { it > 0L } ?: episode.durationMs ?: 0L,
                            // The polling loop clears buffering once the source is ready. Do not
                            // clear it here while a remote source may still be preparing.
                            isBuffering = _state.value.isBuffering,
                            playbackError = null,
                        )
                        settings?.setPodcastLastEpisodeId(episode.id)
                    }
                }
            }.also { listener -> controller?.addListener(listener) }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun play(episode: EpisodeEntity, podcastTitle: String? = null) {
        playQueue(
            episodes = listOf(episode),
            selectedEpisode = episode,
            podcastTitles = mapOf(episode.podcastId to podcastTitle.orEmpty()),
            useVideo = (episode.preferVideo || (!episode.videoPreferenceSet && episode.isVideoOnlySource())) &&
                !episode.videoUrl.isNullOrBlank(),
        )
    }

    fun playFromNotification(episode: EpisodeEntity, podcastTitle: String) {
        if (controller == null) {
            pendingNotificationPlay = episode to podcastTitle
        } else {
            play(episode, podcastTitle)
        }
    }

    fun watch(episode: EpisodeEntity, podcastTitle: String? = null) {
        if (episode.videoUrl.isNullOrBlank()) return
        viewModelScope.launch { repository?.setEpisodePreferVideo(episode.id, true) }
        playQueue(
            episodes = listOf(episode),
            selectedEpisode = episode,
            podcastTitles = mapOf(episode.podcastId to podcastTitle.orEmpty()),
            useVideo = true,
        )
    }

    fun playQueue(
        episodes: List<EpisodeEntity>,
        selectedEpisode: EpisodeEntity,
        podcastTitles: Map<Long, String>,
        useVideo: Boolean = false,
    ) {
        val selectedIndex = episodes.indexOfFirst { it.id == selectedEpisode.id }
        if (selectedIndex < 0) return
        playlistEpisodes = episodes.associateBy(EpisodeEntity::id)
        playlistPodcastTitles = podcastTitles
        videoMode = useVideo
        foregroundVideoPreference = useVideo
        activeSourceIsVideo = useVideo
        displayedPositionMs = selectedEpisode.positionMs
        displayedDurationMs = selectedEpisode.durationMs ?: 0L
        _state.value = PlaybackUiState(
            episode = selectedEpisode,
            isPlaying = true,
            speed = preferredSpeed,
            skipSilence = _state.value.skipSilence,
            positionMs = selectedEpisode.positionMs,
            durationMs = selectedEpisode.durationMs ?: 0L,
            isVideoMode = useVideo,
            // Publish buffering before Media3's first polling tick so remote playback does not
            // briefly show Play/equalizer on the episode detail button.
            isBuffering = selectedEpisode.localUri == null,
            playbackError = null,
        )
        viewModelScope.launch { settings?.setPodcastLastEpisodeId(selectedEpisode.id) }
        viewModelScope.launch {
            val persistedPosition = repository?.episode(selectedEpisode.id)?.positionMs
                ?: selectedEpisode.positionMs
            val podcast = repository?.podcast(selectedEpisode.podcastId)
            val effectiveSpeed = podcast?.playbackSpeed ?: preferredSpeed
            val effectiveSkipSilence = podcast?.skipSilence
                ?: settings?.podcastSkipSilence?.first()
                ?: false
            displayedPositionMs = persistedPosition
            _state.value = _state.value.copy(
                positionMs = persistedPosition,
                speed = effectiveSpeed,
                skipSilence = effectiveSkipSilence,
            )
            val mediaItems = episodes.map { episode ->
                val persistedEpisode = repository?.episode(episode.id) ?: episode
                buildMediaItem(persistedEpisode, useVideo, podcastTitles)
            }
            controller?.apply {
                setMediaItems(mediaItems, selectedIndex, persistedPosition)
                setPlaybackSpeed(effectiveSpeed)
                sendSkipSilenceCommand(effectiveSkipSilence)
                prepare()
                play()
            }
        }
    }

    private fun restoreLastEpisode(episode: EpisodeEntity) {
        playlistEpisodes = mapOf(episode.id to episode)
        playlistPodcastTitles = emptyMap()
        videoMode = false
        foregroundVideoPreference = false
        activeSourceIsVideo = false
        displayedPositionMs = episode.positionMs
        displayedDurationMs = episode.durationMs ?: 0L
        _state.value = PlaybackUiState(
            episode = episode,
            isPlaying = false,
            speed = preferredSpeed,
            skipSilence = _state.value.skipSilence,
            positionMs = episode.positionMs,
            durationMs = episode.durationMs ?: 0L,
            isVideoMode = false,
        )
        viewModelScope.launch {
            val item = buildMediaItem(episode, useVideo = false, playlistPodcastTitles)
            val podcast = repository?.podcast(episode.podcastId)
            val effectiveSpeed = podcast?.playbackSpeed ?: preferredSpeed
            val effectiveSkipSilence = podcast?.skipSilence
                ?: settings?.podcastSkipSilence?.first()
                ?: false
            _state.value = _state.value.copy(skipSilence = effectiveSkipSilence)
            controller?.apply {
                setMediaItem(item, episode.positionMs)
                setPlaybackSpeed(effectiveSpeed)
                sendSkipSilenceCommand(effectiveSkipSilence)
                prepare()
            }
        }
    }

    private suspend fun syncCurrentEpisode() {
        val mediaController = controller ?: return
        val episodeId = mediaController.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val episode = repository?.episode(episodeId) ?: return
        val podcast = repository?.podcast(episode.podcastId)
        val effectiveSpeed = podcast?.playbackSpeed ?: preferredSpeed
        val effectiveSkipSilence = podcast?.skipSilence
            ?: settings?.podcastSkipSilence?.first()
            ?: false
        playlistEpisodes = playlistEpisodes + (episode.id to episode)
        displayedPositionMs = mediaController.currentPosition.coerceAtLeast(0L)
        displayedDurationMs = mediaController.duration.takeIf { it > 0L } ?: episode.durationMs ?: 0L
        _state.value = _state.value.copy(
            episode = episode,
            isPlaying = mediaController.isPlaying,
            positionMs = displayedPositionMs,
            durationMs = displayedDurationMs,
            speed = effectiveSpeed,
            skipSilence = effectiveSkipSilence,
            isBuffering = mediaController.playbackState == Player.STATE_BUFFERING &&
                mediaController.playWhenReady,
        )
        mediaController.setPlaybackSpeed(effectiveSpeed)
        sendSkipSilenceCommand(effectiveSkipSilence)
    }

    fun setVideoMode(enabled: Boolean) {
        val episode = _state.value.episode ?: return
        if (enabled && episode.videoUrl.isNullOrBlank()) return
        if (videoMode == enabled) return
        val mediaController = controller ?: return
        foregroundVideoPreference = enabled

        if (activeSourceIsVideo && (isHlsVideo(episode) || episode.audioUrl == episode.videoUrl)) {
            if (!enabled) clearVideoOutput()
            videoMode = enabled
            _state.value = _state.value.copy(isVideoMode = enabled)
            viewModelScope.launch { repository?.setEpisodePreferVideo(episode.id, enabled) }
            return
        }

        val position = mediaController.currentPosition.coerceAtLeast(0L)
        displayedPositionMs = position
        mediaController.duration.takeIf { it > 0L }?.let { displayedDurationMs = it }
        val wasPlaying = mediaController.isPlaying
        videoMode = enabled
        activeSourceIsVideo = enabled
        _state.value = _state.value.copy(isVideoMode = enabled)
        viewModelScope.launch { repository?.setEpisodePreferVideo(episode.id, enabled) }
        val currentIndex = mediaController.currentMediaItemIndex.takeIf { it >= 0 } ?: 0
        // Keep the existing queue and replace only the active source. Rebuilding
        // the whole playlist makes a simple audio/video toggle unnecessarily slow.
        viewModelScope.launch {
            mediaController.replaceMediaItem(
                currentIndex,
                buildMediaItem(episode, enabled, playlistPodcastTitles),
            )
            mediaController.seekTo(position)
            if (wasPlaying) mediaController.play()
        }
    }

    /** Restores the user's foreground audio/video choice after temporary minimization. */
    fun restoreForegroundVideoPreference() {
        if (foregroundVideoPreference && !_state.value.isVideoMode) {
            setVideoMode(true)
        }
    }

    /** Switches the active queue to audio before the player is minimized. */
    fun switchToAudioForBackground() {
        if (_state.value.episode == null) return
        // Keep the prepared item for instant restoration. This intentionally
        // prioritizes switching speed over reducing video bandwidth in the mini player.
        clearVideoOutput()
        videoMode = false
        _state.value = _state.value.copy(isVideoMode = false)
    }

    private fun isHlsVideo(episode: EpisodeEntity): Boolean =
        episode.videoMimeType.orEmpty().contains("mpegurl", ignoreCase = true) ||
            episode.videoUrl.orEmpty().substringBefore('?').endsWith(".m3u8", ignoreCase = true)

    private suspend fun buildMediaItem(
        episode: EpisodeEntity,
        useVideo: Boolean,
        podcastTitles: Map<Long, String>,
    ): MediaItem {
        val mediaUrl = if (useVideo) {
            // HLS is stream-only for now. Ignore any old manifest file marker from the previous
            // downloader, which downloaded only the playlist rather than its media segments.
            if (isHlsVideo(episode)) episode.videoUrl ?: episode.audioUrl
            else episode.localVideoUri ?: episode.videoUrl ?: episode.audioUrl
        } else resolveAudioUri(episode)
        val mediaMimeType = if (useVideo) episode.videoMimeType else episode.mimeType
        return MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(mediaUrl)
            .setMimeType(mediaMimeType)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setSubtitle(podcastTitles[episode.podcastId])
                    .setArtist(podcastTitles[episode.podcastId])
                    .setAlbumTitle(podcastTitles[episode.podcastId])
                    .setArtworkUri(episode.artworkUrl?.let(android.net.Uri::parse))
                    .build(),
            )
            .build()
    }

    private suspend fun resolveAudioUri(episode: EpisodeEntity): String {
        val candidates = buildList {
            episode.localUri?.let(::add)
            repository?.downloadAsset(episode.id, com.shapeshed.booth.data.DownloadAssetType.AUDIO)
                ?.takeIf { it.status == DownloadAssetStatus.COMPLETED }
                ?.destinationUri
                ?.let(::add)
        }
        return candidates.firstOrNull { uri ->
            File(uri.toUri().path.orEmpty()).isFile
        } ?: episode.audioUrl
    }

    fun pause() { controller?.pause() }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun retryPlayback() {
        val mediaController = controller ?: return
        _state.value = _state.value.copy(playbackError = null, isBuffering = true)
        mediaController.prepare()
        mediaController.play()
    }

    fun clearRememberedEpisode() {
        viewModelScope.launch { settings?.clearPodcastLastEpisodeId() }
    }

    /** Detaches any video output while keeping audio playback and its position intact. */
    fun clearVideoOutput() {
        controller?.clearVideoSurface()
    }

    fun stopAndClear() {
        controller?.apply {
            clearVideoSurface()
            stop()
            clearMediaItems()
        }
        playlistEpisodes = emptyMap()
        playlistPodcastTitles = emptyMap()
        videoMode = false
        foregroundVideoPreference = false
        activeSourceIsVideo = false
        displayedPositionMs = 0L
        displayedDurationMs = 0L
        _state.value = PlaybackUiState(speed = preferredSpeed, skipSilence = _state.value.skipSilence)
        _isPlaying.value = false
    }

    fun setSpeed(speed: Float) {
        val normalizedSpeed = speed.coerceIn(0.5f, 3f)
        preferredSpeed = normalizedSpeed
        viewModelScope.launch {
            settings?.setPodcastPlaybackSpeed(normalizedSpeed)
            val podcastOverride = _state.value.episode
                ?.let { repository?.podcast(it.podcastId)?.playbackSpeed }
            val effectiveSpeed = podcastOverride ?: normalizedSpeed
            controller?.setPlaybackSpeed(effectiveSpeed)
            _state.value = _state.value.copy(speed = effectiveSpeed)
        }
    }

    fun setPodcastPlaybackSpeed(podcastId: Long, speed: Float?) {
        val normalizedSpeed = speed?.coerceIn(0.5f, 3f)
        viewModelScope.launch {
            repository?.setPodcastPlaybackSpeed(podcastId, normalizedSpeed)
        }
        if (_state.value.episode?.podcastId == podcastId) {
            val effectiveSpeed = normalizedSpeed ?: preferredSpeed
            controller?.setPlaybackSpeed(effectiveSpeed)
            _state.value = _state.value.copy(speed = effectiveSpeed)
        }
    }

    fun setSkipSilence(enabled: Boolean) {
        sendSkipSilenceCommand(enabled)
        _state.value = _state.value.copy(skipSilence = enabled)
        viewModelScope.launch {
            settings?.setPodcastSkipSilence(enabled)
        }
    }

    fun setPodcastSkipSilence(podcastId: Long, enabled: Boolean?) {
        viewModelScope.launch {
            repository?.setPodcastSkipSilence(podcastId, enabled)
            if (_state.value.episode?.podcastId == podcastId) {
                val effective = enabled ?: settings?.podcastSkipSilence?.first() ?: false
                sendSkipSilenceCommand(effective)
                _state.value = _state.value.copy(skipSilence = effective)
            }
        }
    }

    private fun sendSkipSilenceCommand(enabled: Boolean) {
        controller?.sendCustomCommand(
            SessionCommand(ACTION_SKIP_SILENCE_SET, Bundle.EMPTY),
            Bundle().apply { putBoolean(SKIP_SILENCE_ENABLED, enabled) },
        )
    }

    fun setSleepTimer(durationMs: Long) {
        controller?.sendCustomCommand(
            SessionCommand(ACTION_SLEEP_TIMER_SET, Bundle.EMPTY),
            Bundle().apply { putLong(SLEEP_TIMER_DURATION_MS, durationMs) },
        )
    }

    fun cancelSleepTimer() {
        controller?.sendCustomCommand(
            SessionCommand(ACTION_SLEEP_TIMER_CANCEL, Bundle.EMPTY),
            Bundle.EMPTY,
        )
    }

    fun seekBack() { controller?.seekBack() }

    fun seekForward() { controller?.seekForward() }

    fun seekTo(positionMs: Long) {
        displayedPositionMs = positionMs.coerceAtLeast(0L)
        controller?.seekTo(displayedPositionMs)
    }

    fun resetPositionIfCurrent(episodeId: Long) {
        val mediaController = controller
        if (mediaController?.currentMediaItem?.mediaId != episodeId.toString()) {
            Log.d("PodcastPlayback", "reset ignored episode=$episodeId current=${mediaController?.currentMediaItem?.mediaId}")
            return
        }
        Log.d(
            "PodcastPlayback",
            "reset requested episode=$episodeId position=${mediaController.currentPosition} isPlaying=${mediaController.isPlaying}",
        )
        displayedPositionMs = 0L
        mediaController.pause()
        mediaController.seekTo(0L)
        _isPlaying.value = false
        _state.value = _state.value.copy(
            positionMs = 0L,
            isPlaying = false,
            isBuffering = false,
        )
    }

    override fun onCleared() {
        controllerListener?.let { listener -> controller?.removeListener(listener) }
        controllerListener = null
        controller?.release()
        controller = null
        controllerFuture?.let(MediaController:: releaseFuture)
        controllerFuture = null
    }
}

internal fun PlaybackException.userMessage(): PodcastUiError = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    -> PodcastUiError.PlaybackConnectionFailed
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        PodcastUiError.PlaybackServerFailed
    else -> PodcastUiError.PlaybackFailed
}

internal fun EpisodeEntity.isVideoOnlySource(): Boolean =
    audioUrl == videoUrl &&
        (mimeType.orEmpty().startsWith("video/", ignoreCase = true) ||
            videoMimeType.orEmpty().startsWith("video/", ignoreCase = true))
