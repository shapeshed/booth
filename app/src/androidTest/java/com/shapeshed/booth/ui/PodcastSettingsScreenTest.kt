package com.shapeshed.booth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.booth.data.PodcastEntity
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PodcastSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun automaticEpisodeTogglesPersistTheirPodcastPreferences() {
        var savedAutoRefresh: Boolean? = null
        composeRule.setContent {
            BoothAppTheme {
                PodcastSettingsScreen(
                    podcast = testPodcast,
                    globalPlaybackSpeed = 1f,
                    globalSkipSilence = false,
                    onPlaybackSpeedChange = {},
                    onSkipSilenceChange = {},
                    globalAutoQueueEnabled = false,
                    globalNotificationsEnabled = false,
                    availableTags = emptyList(),
                    onSaveSettings = { _, _, _, refresh, _, _, _ ->
                        savedAutoRefresh = refresh
                    },
                )
            }
        }

        composeRule.onNodeWithText("Refresh podcasts automatically").assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertEquals(false, savedAutoRefresh) }

        composeRule.onNodeWithText("Download episodes added to Up Next").assertDoesNotExist()
    }

    @Test
    fun podcastVideoPreferenceIsNotShown() {
        composeRule.setContent {
            BoothAppTheme {
                PodcastSettingsScreen(
                    podcast = testPodcast,
                    globalPlaybackSpeed = 1f,
                    globalSkipSilence = false,
                    onPlaybackSpeedChange = {},
                    onSkipSilenceChange = {},
                    globalAutoQueueEnabled = true,
                    globalNotificationsEnabled = true,
                    availableTags = emptyList(),
                    onSaveSettings = { _, _, _, _, _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Download videos when available").assertDoesNotExist()
    }

    @Test
    fun playbackOverridesCanReturnToGlobalDefaults() {
        var playbackSpeed: Float? = 1.5f
        var skipSilence: Boolean? = true
        composeRule.setContent {
            BoothAppTheme {
                PodcastSettingsScreen(
                    podcast = testPodcast.copy(playbackSpeed = 1.5f, skipSilence = true),
                    globalPlaybackSpeed = 1f,
                    globalSkipSilence = false,
                    onPlaybackSpeedChange = { playbackSpeed = it },
                    onSkipSilenceChange = { skipSilence = it },
                    globalAutoQueueEnabled = false,
                    globalNotificationsEnabled = false,
                    availableTags = emptyList(),
                    onSaveSettings = { _, _, _, _, _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Playback speed").performClick()
        composeRule.onNodeWithText("Use global skip silence").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(null, skipSilence) }
        composeRule.onNodeWithText("Use global playback speed").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(null, playbackSpeed) }
    }

    private companion object {
        val testPodcast = PodcastEntity(
            id = 1L,
            title = "Test podcast",
            author = null,
            feedUrl = "https://example.com/feed.xml",
            siteUrl = null,
            descriptionHtml = null,
            artworkUrl = null,
            subscribedAtMillis = 0L,
            lastRefreshMillis = null,
        )
    }
}
