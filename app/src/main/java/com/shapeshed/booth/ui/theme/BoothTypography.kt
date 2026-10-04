package com.shapeshed.booth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle

/**
 * Named text roles, so a given piece of text has one style to reach for.
 *
 * The app already routed every string through `MaterialTheme.typography` rather than raw font sizes,
 * which is why this is a naming problem rather than a consistency one in the strict sense. But the
 * same *role* had picked different styles in different places: a section heading inside scrollable
 * content was `headlineSmall` on the podcast screen, `titleMedium` in search and `labelLarge` in
 * settings. Three answers to one question.
 *
 * These are a thin mapping onto the Material 3 scale rather than a custom type scale. The stock scale
 * is deliberate and worth keeping; what was missing was a record of which rung each role sits on, so
 * the next screen added does not have to guess and a reviewer has something to hold it to.
 *
 * A role is only worth adding here when more than one screen has a use for it. A one-off heading
 * should use `MaterialTheme.typography` directly rather than inventing a role.
 */
object BoothTypography {
    /**
     * A heading for a section within a piece of scrolling content: "About this podcast",
     * "Description", the groups in search.
     *
     * Below a screen's own title on purpose, because it labels part of a screen rather than the
     * screen itself.
     */
    val sectionHeader: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium

    /**
     * The title of an episode or podcast inside a list row or a detail header.
     *
     * The largest text on a row, and the thing a scan of the list actually reads.
     */
    val rowTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium

    /**
     * The quiet line that qualifies a row: its date, its size, its download state.
     *
     * [rowTitle] carries the meaning; this only disambiguates, so it must not compete with it.
     */
    val rowMeta: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelMedium

    /** Running prose: show notes, descriptions, empty-state explanations. */
    val body: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyLarge

    /** Supporting prose under a block: form summaries, secondary explanations. */
    val caption: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall
}
