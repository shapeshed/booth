package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inbox
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.shapeshed.booth.R

/**
 * Test tag for the unseen-episodes dot.
 *
 * The dot is a decorative `Badge` with no semantics and no text, so there is nothing else to assert
 * on: without a tag the only way to test it would be a screenshot. The tag is added only when the dot
 * is drawn, so its presence *is* the assertion.
 */
internal const val INBOX_BADGE_TEST_TAG = "inbox-unseen-badge"

@Composable
internal fun PodcastHomeBottomNavigation(
    visible: Boolean,
    selectedTab: PodcastTab,
    hasUnseenInboxEpisodes: Boolean,
    onSelectTab: (PodcastTab) -> Unit,
) {
    if (!visible) return
    ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        PodcastHomeNavigationItems(
            selectedTab = selectedTab,
            hasUnseenInboxEpisodes = hasUnseenInboxEpisodes,
            onSelectTab = onSelectTab,
            tabItem = { selected, onClick, icon, label ->
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
    hasUnseenInboxEpisodes: Boolean,
    onSelectTab: (PodcastTab) -> Unit,
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
                hasUnseenInboxEpisodes = hasUnseenInboxEpisodes,
                onSelectTab = onSelectTab,
                tabItem = { selected, onClick, icon, label ->
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
    hasUnseenInboxEpisodes: Boolean,
    onSelectTab: (PodcastTab) -> Unit,
    tabItem: @Composable (
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        label: @Composable () -> Unit,
    ) -> Unit,
) {
    tabItem(
        selectedTab == PodcastTab.HOME,
        { onSelectTab(PodcastTab.HOME) },
        {
            BadgedBox(
                badge = {
                    if (hasUnseenInboxEpisodes) Badge(modifier = Modifier.testTag(INBOX_BADGE_TEST_TAG))
                },
            ) {
                Icon(Icons.Rounded.Inbox, contentDescription = null)
            }
        },
        { Text(stringResource(R.string.inbox)) },
    )
    tabItem(
        selectedTab == PodcastTab.UP_NEXT,
        { onSelectTab(PodcastTab.UP_NEXT) },
        { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null) },
        { Text(stringResource(R.string.up_next)) },
    )
    tabItem(
        selectedTab == PodcastTab.SUBSCRIPTIONS,
        { onSelectTab(PodcastTab.SUBSCRIPTIONS) },
        { Icon(Icons.Filled.RssFeed, contentDescription = null) },
        { Text(stringResource(R.string.podcast_subscriptions)) },
    )
}

@Composable
internal fun PodcastHomeDiscoverFab(visible: Boolean, bottomInset: Dp, onClick: () -> Unit) {
    if (!visible) return
    Box(modifier = Modifier.padding(bottom = bottomInset)) {
        FloatingActionButton(onClick = onClick) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.discover_podcasts))
        }
    }
}
