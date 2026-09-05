package com.shapeshed.booth.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerTest {
    @Test
    fun formatsPlaybackSpeed() {
        assertEquals("0.5×", formatPlaybackSpeed(0.5f))
        assertEquals("1×", formatPlaybackSpeed(1f))
        assertEquals("1.25×", formatPlaybackSpeed(1.25f))
    }

    @Test
    fun identifiesStandardPlaybackSpeeds() {
        assertEquals(true, isStandardPlaybackSpeed(1.25f))
        assertEquals(false, isStandardPlaybackSpeed(1.3f))
    }

    @Test
    fun formatsMinutesWithRoundedSeconds() {
        assertEquals("15:00", formatSleepRemaining(15 * 60_000L))
        assertEquals("14:01", formatSleepRemaining(14 * 60_000L + 1_000L))
        assertEquals("0:01", formatSleepRemaining(1L))
    }

    @Test
    fun formatsHoursWithTwoDigitMinutesAndSeconds() {
        assertEquals("1:05:09", formatSleepRemaining(1 * 3_600_000L + 5 * 60_000L + 9_000L))
    }
}
