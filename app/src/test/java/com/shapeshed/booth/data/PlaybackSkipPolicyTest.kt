package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSkipPolicyTest {
    @Test fun introSkipsFreshEpisode() = assertEquals(30_000L, PlaybackSkipPolicy.introPosition(0L, 30_000L, 600_000L))
    @Test fun introKeepsSavedPosition() = assertEquals(45_000L, PlaybackSkipPolicy.introPosition(45_000L, 30_000L, 600_000L))
    @Test fun introDoesNotSkipPastDuration() = assertEquals(0L, PlaybackSkipPolicy.introPosition(0L, 30_000L, 20_000L))
    @Test fun endingSkipsWhenPlaybackEntersConfiguredEnding() = assertTrue(
        PlaybackSkipPolicy.shouldSkipEnding(540_000L, 600_000L, 60_000L, 1f),
    )
    @Test fun endingDoesNotSkipBeforeConfiguredEnding() = assertFalse(
        PlaybackSkipPolicy.shouldSkipEnding(539_999L, 600_000L, 60_000L, 1f),
    )
    @Test fun endingDoesNotSkipWhenConfiguredEndingCoversEpisode() = assertFalse(
        PlaybackSkipPolicy.shouldSkipEnding(590_000L, 600_000L, 600_000L, 1f),
    )
}
