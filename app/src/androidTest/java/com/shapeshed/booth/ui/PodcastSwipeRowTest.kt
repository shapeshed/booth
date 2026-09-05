package com.shapeshed.booth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
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
        composeRule.setContent {
            BoothAppTheme {
                PodcastSwipeRow(
                    podcast = podcast(),
                    onClick = {},
                    onRemove = { removeCount++ },
                )
            }
        }

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("Remove Test podcast").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle { assertEquals(0, removeCount) }

        composeRule.onRoot().performTouchInput { swipeLeft() }
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
