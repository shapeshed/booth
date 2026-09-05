package com.shapeshed.booth.ui

internal const val NAVIGATION_RAIL_BREAKPOINT_DP = 600
internal const val TWO_PANE_BREAKPOINT_DP = 840

internal data class PodcastAdaptiveLayout(
    val useNavigationRail: Boolean,
    val useTwoPaneEpisodeLayout: Boolean,
)

/** Keeps product layout decisions separate from WindowSizeClass and therefore unit-testable. */
internal fun podcastAdaptiveLayout(
    isWidthAtLeastBreakpoint: (Int) -> Boolean,
): PodcastAdaptiveLayout = PodcastAdaptiveLayout(
    useNavigationRail = isWidthAtLeastBreakpoint(NAVIGATION_RAIL_BREAKPOINT_DP),
    useTwoPaneEpisodeLayout = isWidthAtLeastBreakpoint(TWO_PANE_BREAKPOINT_DP),
)
