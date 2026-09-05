package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/** Stateless shell shared by the home route's toolbar, navigation, content, and overlays. */
@Composable
internal fun PodcastHomeAppContent(
    modifier: Modifier = Modifier,
    navigationRailStartPadding: Dp,
    snackbarHost: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .padding(start = navigationRailStartPadding),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        snackbarHost = snackbarHost,
        topBar = topBar,
        bottomBar = bottomBar,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(padding)
        }
    }
}
