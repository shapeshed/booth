package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The override rule between a podcast's own playback setting and the app-wide default.
 *
 * These exist because both failure modes are silent. A speed override that collapsed 1.0 into the
 * global would make it impossible to keep one feed at normal speed, and a skip-silence override that
 * treated `false` as "unset" would make it impossible to keep silence in one feed. Neither crashes
 * and neither looks wrong on screen; the only symptom is the wrong audio.
 */
class PlaybackSettingsTest {
    @Test
    fun aPodcastWithoutASpeedFollowsTheGlobalOne() {
        assertEquals(1.5f, PlaybackSettings.effectiveSpeed(null, 1.5f), 0f)
    }

    @Test
    fun aPodcastWithASpeedOverridesTheGlobalOne() {
        assertEquals(0.75f, PlaybackSettings.effectiveSpeed(0.75f, 1.5f), 0f)
    }

    @Test
    fun aPodcastSetToExactlyNormalSpeedIsAnOverrideNotAnAbsence() {
        // The case that breaks a "treat the default as unset" implementation. Setting the global to
        // 1.5 must not drag this feed along with it, because 1.0 was chosen for this feed.
        assertEquals(1f, PlaybackSettings.effectiveSpeed(PlaybackSettings.DEFAULT_SPEED, 1.5f), 0f)
    }

    @Test
    fun skipSilenceFollowsTheGlobalWhenThePodcastHasNone() {
        assertTrue(PlaybackSettings.effectiveSkipSilence(null, true))
        assertFalse(PlaybackSettings.effectiveSkipSilence(null, false))
    }

    @Test
    fun aPodcastTurningSkipSilenceOffOverridesTheGlobalTurningItOn() {
        // The nullable-boolean trap. `false` here is a decision, and it is the only way to keep
        // silence in one feed while skipping it everywhere else.
        assertFalse(PlaybackSettings.effectiveSkipSilence(podcastSkipSilence = false, globalSkipSilence = true))
    }

    @Test
    fun aPodcastTurningSkipSilenceOnOverridesTheGlobalTurningItOff() {
        assertTrue(PlaybackSettings.effectiveSkipSilence(podcastSkipSilence = true, globalSkipSilence = false))
    }

    @Test
    fun clampKeepsASpeedThePlayerCanRepresent() {
        assertEquals(0.5f, PlaybackSettings.clampSpeed(0.1f), 0f)
        assertEquals(3f, PlaybackSettings.clampSpeed(4f), 0f)
        assertEquals(1.25f, PlaybackSettings.clampSpeed(1.25f), 0f)
    }

    @Test
    fun theDefaultIsInsideTheClampSoItSurvivesTheRoundTrip() {
        // If the default were ever moved outside the range, every read would clamp it to a
        // different number than was written, and the setting would look like it had been changed.
        assertEquals(
            PlaybackSettings.DEFAULT_SPEED,
            PlaybackSettings.clampSpeed(PlaybackSettings.DEFAULT_SPEED),
            0f,
        )
    }
}
