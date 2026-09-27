package com.shapeshed.booth.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two "is this downloaded" predicates exist because there are two different questions, and the
 * codebase previously answered each of them with its own inline copy of a similar expression in ten
 * places. A divergence in the retention one leaks or deletes the wrong files, so they are named and
 * pinned here.
 */
class EpisodeDownloadStateTest {
    private fun episode(localUri: String? = null, localVideoUri: String? = null) = EpisodeEntity(
        id = 1L,
        podcastId = 1L,
        guid = "guid",
        title = "Episode",
        descriptionHtml = null,
        audioUrl = "https://example.com/a.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = null,
        localUri = localUri,
        localVideoUri = localVideoUri,
        positionMs = 0L,
        completed = false,
        inInbox = false,
        firstSeenAtMillis = null,
    )

    @Test
    fun noLocalMediaWhenNeitherUriIsSet() {
        assertFalse(episode().hasLocalMedia())
    }

    @Test
    fun audioAloneCountsAsLocalMedia() {
        assertTrue(episode(localUri = "file:///a.mp3").hasLocalMedia())
    }

    /** Retention must treat a video-only download as reclaimable too, or the file leaks. */
    @Test
    fun videoAloneCountsAsLocalMedia() {
        assertTrue(episode(localVideoUri = "file:///a.mp4").hasLocalMedia())
    }

    @Test
    fun noLocalMediaWhenBothUrisAreBlankStrings() {
        // A blank string is not a usable path, and is what an interrupted download can leave behind.
        assertFalse(episode(localUri = "", localVideoUri = "").hasLocalMedia())
    }

    @Test
    fun isDownloadedTracksAudioColumn() {
        assertTrue(episode(localUri = "file:///a.mp3").isDownloaded(null))
        assertFalse(episode().isDownloaded(null))
    }

    /**
     * The reason this is not the same as [hasLocalMedia]: progress is published when a transfer
     * completes, before the episode row's localUri is written, so gating only on the column makes a
     * finished download look unavailable for a moment.
     */
    @Test
    fun isDownloadedIsTrueWhenProgressJustCompleted() {
        val progress = DownloadProgress(
            bytesDownloaded = 1_000L,
            totalBytes = 1_000L,
            startedAtElapsedMs = 0L,
            completed = true,
        )

        assertTrue(episode().isDownloaded(progress))
    }

    @Test
    fun isDownloadedIsFalseForAnInFlightTransfer() {
        val progress = DownloadProgress(
            bytesDownloaded = 500L,
            totalBytes = 1_000L,
            startedAtElapsedMs = 0L,
            completed = false,
        )

        assertFalse(episode().isDownloaded(progress))
    }

    /** The UI affordance is audio only, so a video-only download should not claim to be playable. */
    @Test
    fun isDownloadedIgnoresVideoOnly() {
        assertFalse(episode(localVideoUri = "file:///a.mp4").isDownloaded(null))
    }
}
