package com.shapeshed.booth.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastAdaptiveLayoutTest {
    @Test
    fun compactWindowUsesBottomNavigationAndSinglePane() {
        val layout = podcastAdaptiveLayout { width -> width <= 599 }

        assertFalse(layout.useNavigationRail)
        assertFalse(layout.useTwoPaneEpisodeLayout)
    }

    @Test
    fun mediumWindowUsesRailButRemainsSinglePane() {
        val layout = podcastAdaptiveLayout { width -> width <= 600 }

        assertTrue(layout.useNavigationRail)
        assertFalse(layout.useTwoPaneEpisodeLayout)
    }

    @Test
    fun expandedWindowUsesRailAndTwoPaneLayout() {
        val layout = podcastAdaptiveLayout { width -> width <= 840 }

        assertTrue(layout.useNavigationRail)
        assertTrue(layout.useTwoPaneEpisodeLayout)
    }
}
