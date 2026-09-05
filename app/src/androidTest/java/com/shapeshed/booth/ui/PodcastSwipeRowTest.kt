package com.shapeshed.booth.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PodcastSwipeRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun swipingPodcastRequiresConfirmation() {
        var removeCount = 0
        var contentKey by mutableStateOf(0)
        composeRule.setContent {
            BoothAppTheme {
                key(contentKey) {
                    PodcastSwipeRow(
                        podcast = podcast(),
                        onClick = {},
                        onRemove = { removeCount++ },
                        initiallyShowRemovalConfirmation = true,
                    )
                }
            }
        }

        composeRule.onNodeWithText("Remove Test podcast").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle { assertEquals(0, removeCount) }

        composeRule.runOnIdle { contentKey++ }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Remove Test podcast").assertIsDisplayed()
        composeRule.onNodeWithText("Remove").performClick()
        composeRule.runOnIdle { assertEquals(1, removeCount) }
    }

    private fun podcast() = PodcastEntity(
        id = 1L,
        title = "Test podcast",
        author = "Test author",
        feedUrl = "https://example.com/feed.xml",
        siteUrl = null,
        descriptionHtml = null,
        artworkUrl = null,
        subscribedAtMillis = 1L,
        lastRefreshMillis = null,
    )

}
