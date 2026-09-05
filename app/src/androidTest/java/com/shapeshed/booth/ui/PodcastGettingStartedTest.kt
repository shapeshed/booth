package com.shapeshed.booth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PodcastGettingStartedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyLibraryOffersSearchAndImport() {
        var searchClicks = 0
        var importClicks = 0
        composeRule.setContent {
            BoothAppTheme {
                PodcastGettingStarted(
                    popularPodcasts = emptyList(),
                    isLoadingPopular = false,
                    onSearch = { searchClicks++ },
                    onImportOpml = { importClicks++ },
                    onOpenPodcast = {},
                )
            }
        }

        composeRule.onNodeWithText("Start listening").assertIsDisplayed()
        composeRule.onNodeWithText("Search podcasts").performClick()
        composeRule.onNodeWithText("Import OPML").performClick()

        composeRule.runOnIdle {
            assertEquals(1, searchClicks)
            assertEquals(1, importClicks)
        }
    }
}
