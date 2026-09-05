package com.shapeshed.booth.ui

import androidx.media3.common.PlaybackException
import com.shapeshed.booth.data.EpisodeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastPlaybackBehaviorTest {
    @Test
    fun networkErrorsUseRecoverableConnectionMessage() {
        assertEquals(
            PodcastUiError.PlaybackConnectionFailed,
            PlaybackException("timeout", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
                .userMessage(),
        )
        assertEquals(
            PodcastUiError.PlaybackConnectionFailed,
            PlaybackException("offline", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
                .userMessage(),
        )
    }

    @Test
    fun badHttpAndUnknownErrorsHaveStableMessages() {
        assertEquals(
            PodcastUiError.PlaybackServerFailed,
            PlaybackException("404", null, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS).userMessage(),
        )
        assertEquals(
            PodcastUiError.PlaybackFailed,
            PlaybackException("unknown", null, PlaybackException.ERROR_CODE_UNSPECIFIED).userMessage(),
        )
    }

    @Test
    fun videoOnlySourceIsDetectedFromMatchingVideoMimeTypes() {
        assertTrue(episode(mimeType = "video/mp4", videoMimeType = "video/mp4").isVideoOnlySource())
        assertTrue(episode(mimeType = "audio/mpeg", videoMimeType = "video/mp4").isVideoOnlySource())
        assertFalse(episode(mimeType = "audio/mpeg", videoMimeType = "audio/mpeg").isVideoOnlySource())
        assertFalse(episode(mimeType = "video/mp4", videoMimeType = "video/mp4", videoUrl = "other").isVideoOnlySource())
    }

    private fun episode(
        mimeType: String,
        videoMimeType: String,
        videoUrl: String = "same",
    ) = EpisodeEntity(
        id = 1L,
        podcastId = 1L,
        guid = "episode-1",
        title = "Episode",
        descriptionHtml = null,
        audioUrl = "same",
        mimeType = mimeType,
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = null,
        positionMs = 0L,
        completed = false,
        localUri = null,
        inInbox = false,
        firstSeenAtMillis = null,
        videoUrl = videoUrl,
        videoMimeType = videoMimeType,
    )
}
