package com.shapeshed.booth.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PodcastHomeRootActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clearUpNextRequiresConfirmation() {
        var clearQueueClicks = 0
        composeRule.setContent {
            var menuExpanded by remember { mutableStateOf(false) }
            BoothAppTheme {
                PodcastHomeRootActions(
                    selectedTab = PodcastTab.UP_NEXT,
                    queueReorderMode = false,
                    queueHasItems = true,
                    menuExpanded = menuExpanded,
                    onClearInbox = {},
                    onClearQueue = { clearQueueClicks++ },
                    onQueueReorderDone = {},
                    onOpenDiscoverySearch = {},
                    onOpenAddPodcast = {},
                    onMenuExpandedChange = { menuExpanded = it },
                    onOpenDownloads = {},
                    onOpenAllEpisodes = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Clear Up next").performClick()

        composeRule.onNodeWithText("Remove all episodes from Up next? This won’t delete downloaded episodes.")
            .assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, clearQueueClicks) }

        composeRule.onNodeWithText("Clear").performClick()
        composeRule.runOnIdle { assertEquals(1, clearQueueClicks) }
    }

    @Test
    fun clearUpNextCanBeCancelled() {
        var clearQueueClicks = 0
        composeRule.setContent {
            var menuExpanded by remember { mutableStateOf(false) }
            BoothAppTheme {
                PodcastHomeRootActions(
                    selectedTab = PodcastTab.UP_NEXT,
                    queueReorderMode = false,
                    queueHasItems = true,
                    menuExpanded = menuExpanded,
                    onClearInbox = {},
                    onClearQueue = { clearQueueClicks++ },
                    onQueueReorderDone = {},
                    onOpenDiscoverySearch = {},
                    onOpenAddPodcast = {},
                    onMenuExpandedChange = { menuExpanded = it },
                    onOpenDownloads = {},
                    onOpenAllEpisodes = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Clear Up next").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.runOnIdle { assertEquals(0, clearQueueClicks) }
    }
}
