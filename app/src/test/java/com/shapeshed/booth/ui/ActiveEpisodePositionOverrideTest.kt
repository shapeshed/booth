package com.shapeshed.booth.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The played position shown on an episode row.
 *
 * This exists because the value was computed inline in five places and had already diverged: four
 * sites rescaled the player's position onto the episode's own duration, and one passed the raw
 * position through, so the active episode's ten-minute position was drawn on a two-hour episode's
 * row as though the listener were 8% of the way through it.
 */
class ActiveEpisodePositionOverrideTest {
    private fun override(
        episodeId: Long = 1L,
        activeEpisodeId: Long? = 1L,
        playedFraction: Float? = 0.5f,
        episodeDurationMs: Long? = 120L,
    ) = activeEpisodePositionOverride(
        episodeId = episodeId,
        activeEpisodeId = activeEpisodeId,
        playedFraction = playedFraction,
        episodeDurationMs = episodeDurationMs,
    )

    @Test
    fun rescalesTheFractionOntoTheEpisodesOwnDuration() {
        assertEquals(60L, override(episodeDurationMs = 120L, playedFraction = 0.5f))
    }

    /**
     * The bug this replaced: the player's position is measured against the player's duration, and
     * the row is drawn against the episode's. Ten minutes into a ten-minute episode is not ten
     * minutes into a two-hour one.
     */
    @Test
    fun scalesRatherThanReusingThePlayersRawPosition() {
        // Player is 50% through a 10-minute episode: positionMs would be 300_000. The row belongs to
        // a 2-hour episode, so it must show an hour, not five minutes.
        assertEquals(3_600_000L, override(episodeDurationMs = 7_200_000L, playedFraction = 0.5f))
    }

    @Test
    fun nothingIsDrawnForANonActiveRow() {
        assertNull(override(episodeId = 2L, activeEpisodeId = 1L))
    }

    @Test
    fun nothingIsDrawnWhenNothingIsPlaying() {
        assertNull(override(activeEpisodeId = null))
    }

    /** An unknown active duration means the fraction itself is undefined, so there is no position. */
    @Test
    fun nothingIsDrawnWhenTheFractionIsUnknown() {
        assertNull(override(playedFraction = null))
    }

    @Test
    fun nothingIsDrawnWhenTheEpisodeHasNoDuration() {
        assertNull(override(episodeDurationMs = null))
    }

    @Test
    fun nothingIsDrawnWhenTheEpisodeDurationIsZero() {
        assertNull(override(episodeDurationMs = 0L))
    }

    @Test
    fun nothingIsDrawnWhenTheEpisodeDurationIsNegative() {
        assertNull(override(episodeDurationMs = -1L))
    }

    @Test
    fun theStartAndTheEndOfAnEpisodeAreExact() {
        assertEquals(0L, override(playedFraction = 0f))
        assertEquals(120L, override(playedFraction = 1f))
    }

    /**
     * A stale position past the end, which a seek back can briefly leave behind, must not draw
     * outside the row.
     */
    @Test
    fun clampsToTheEpisodesDuration() {
        assertEquals(120L, override(playedFraction = 1.4f))
    }

    @Test
    fun aNegativeFractionDoesNotDrawBehindTheStart() {
        assertEquals(0L, override(playedFraction = -0.5f))
    }
}
