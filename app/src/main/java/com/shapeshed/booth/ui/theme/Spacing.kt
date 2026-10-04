package com.shapeshed.booth.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The spacing scale, named by role rather than by size.
 *
 * Booth had no spacing tokens: every gap was a `.dp` literal chosen at the call site, which is how the
 * same visual job ended up with four different values. Downloads sat at an 8dp horizontal inset while
 * Inbox and Up next sat at 16dp, so sibling tabs did not line up; the gap between a title and its
 * subtitle was 4dp in one place, 6dp in another and 8dp in a third; and the first row of a list
 * started 0, 8, 12 or 16dp below the bar depending on the screen.
 *
 * Naming by role is the point. `Spacing.md` would leave the same argument to be had again at every
 * call site; `Spacing.listItem` says what the gap is for, so a new list has one obvious value to
 * reach for and a reviewer has something to check it against.
 *
 * The scale is the 4dp grid the app already used most often. Values off it (2.dp in a couple of tight
 * text stacks) are deliberately not represented here: they are optical adjustments at a single call
 * site, and promoting one would invite it to spread.
 */
object Spacing {
    /**
     * A title directly over the line that supports it.
     *
     * Tighter than [inline] on purpose: the two lines are one unit of meaning, and a gap wide enough
     * to read as separation makes the pair look like two unrelated rows.
     */
    val titleStack = 4.dp

    /**
     * Items sitting on one line: the date, the explicit marker, the size, a download indicator.
     *
     * [block] would be far too wide here; this is the horizontal rhythm between peers in a row.
     */
    val inline = 8.dp

    /**
     * Between the rows of a list.
     *
     * The same value the lists already shared through `PODCAST_LIST_ITEM_SPACING`, so this is a
     * rename of an existing standard rather than a new one.
     */
    val listItem = 8.dp

    /** Between blocks within a screen: paragraphs, a heading and the content it introduces. */
    val block = 16.dp

    /**
     * Inside a detail header: the date, the title, the podcast row, the size, the actions.
     *
     * Between [listItem] and [block]. The parts are more separate than the lines of a list row but
     * less separate than the page's top-level blocks, and collapsing them to either value makes the
     * header read as either cramped or scattered.
     */
    val headingStack = 12.dp

    /** Between the top-level sections of a screen. */
    val section = 24.dp

    /** The horizontal inset for screen content, so rows line up across sibling screens. */
    val screenInset = 16.dp

    /**
     * The horizontal inset on a two-pane layout.
     *
     * Wider on purpose: a detail view that shares its width with an artwork pane and a divider needs
     * more breathing room at the edges than a phone-width list, and the extra room is the reason the
     * two-pane layout exists. Kept as a named value so it reads as a decision rather than a typo.
     */
    val screenInsetWide = 24.dp

    /**
     * Where the first row of a list begins below the app bar.
     *
     * Not zero: a list flush to the bar loses the bar's own bottom padding and reads as clipped.
     */
    val listTop = 8.dp

    /**
     * Space reserved at the bottom of a scrolling screen for the mini player.
     *
     * Added to, never replaced by, the screen's own bottom padding, because the mini player overlays
     * the content rather than displacing it.
     */
    val miniPlayer = 16.dp
}
