package com.shapeshed.booth.ui

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun PodcastActionListItem(
    modifier: Modifier = Modifier,
    leadingContent: (@Composable (() -> Unit))? = null,
    supportingContent: (@Composable (() -> Unit))? = null,
    trailingContent: (@Composable (() -> Unit))? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    headlineContent: @Composable () -> Unit,
) {
    ListItem(
        modifier = modifier,
        leadingContent = leadingContent,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
        colors = colors,
    ) {
        Column {
            headlineContent()
        }
    }
}
