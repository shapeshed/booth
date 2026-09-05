package com.shapeshed.booth.ui

import androidx.compose.ui.res.stringResource
import com.shapeshed.booth.R

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
internal fun PodcastHomeBottomNavigation(
    visible: Boolean,
    selectedTab: PodcastTab,
    inboxCount: Int,
    onTabSelected: (PodcastTab) -> Unit,
) {
    if (!visible) return
    ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        PodcastHomeNavigationItems(
            selectedTab = selectedTab,
            inboxCount = inboxCount,
            onTabSelected = onTabSelected,
            item = { selected, onClick, icon, label ->
                ShortNavigationBarItem(
                    selected = selected,
                    onClick = onClick,
                    icon = icon,
                    label = label,
                )
            },
        )
    }
}

@Composable
internal fun PodcastHomeNavigationRail(
    visible: Boolean,
    selectedTab: PodcastTab,
    inboxCount: Int,
    onTabSelected: (PodcastTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            PodcastHomeNavigationItems(
                selectedTab = selectedTab,
                inboxCount = inboxCount,
                onTabSelected = onTabSelected,
                item = { selected, onClick, icon, label ->
                    NavigationRailItem(
                        selected = selected,
                        onClick = onClick,
                        icon = icon,
                        label = label,
                    )
                },
            )
        }
    }
}

@Composable
private fun PodcastHomeNavigationItems(
    selectedTab: PodcastTab,
    inboxCount: Int,
    onTabSelected: (PodcastTab) -> Unit,
    item: @Composable (
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        label: @Composable () -> Unit,
    ) -> Unit,
) {
    item(
        selectedTab == PodcastTab.HOME,
        { onTabSelected(PodcastTab.HOME) },
        {
            BadgedBox(
                badge = {
                    if (inboxCount > 0) Badge { Text(if (inboxCount > 99) "99+" else inboxCount.toString()) }
                },
            ) {
                Icon(Icons.Rounded.Inbox, contentDescription = null)
            }
        },
        { Text(stringResource(R.string.inbox)) },
    )
    item(
        selectedTab == PodcastTab.UP_NEXT,
        { onTabSelected(PodcastTab.UP_NEXT) },
        { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null) },
        { Text(stringResource(R.string.up_next)) },
    )
    item(
        selectedTab == PodcastTab.SUBSCRIPTIONS,
        { onTabSelected(PodcastTab.SUBSCRIPTIONS) },
        { Icon(Icons.Filled.RssFeed, contentDescription = null) },
        { Text(stringResource(R.string.podcast_subscriptions)) },
    )
}

@Composable
internal fun PodcastHomeDiscoverFab(
    visible: Boolean,
    bottomInset: Dp,
    onClick: () -> Unit,
) {
    if (!visible) return
    Box(modifier = Modifier.padding(bottom = bottomInset)) {
        FloatingActionButton(onClick = onClick) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.discover_podcasts))
        }
    }
}
