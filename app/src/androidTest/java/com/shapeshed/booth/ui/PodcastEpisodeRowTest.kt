package com.shapeshed.booth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PodcastEpisodeRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun episodeRowRendersAndInvokesPlay() {
        var playCount = 0
        composeRule.setContent {
            BoothAppTheme {
                EpisodeRow(
                    episode = episode(),
                    onOpen = {},
                    onLongPress = {},
                    onPlay = { playCount++ },
                    onTogglePlayPause = {},
                    active = false,
                    isPlaying = false,
                    onDownload = {},
                    downloadProgress = null,
                )
            }
        }

        composeRule.onNodeWithText("New episode").assertIsDisplayed()
        composeRule.onNodeWithText("Play").performClick()
        composeRule.runOnIdle { assertEquals(1, playCount) }
    }

    @Test
    fun tappingArtworkInvokesEpisodeOpen() {
        var openCount = 0
        composeRule.setContent {
            BoothAppTheme {
                EpisodeRow(
                    episode = episode().copy(artworkUrl = "https://example.com/artwork.jpg"),
                    onOpen = { openCount++ },
                    onLongPress = {},
                    onPlay = {},
                    onTogglePlayPause = {},
                    active = false,
                    isPlaying = false,
                    onDownload = {},
                    downloadProgress = null,
                )
            }
        }

        composeRule.onNodeWithContentDescription("New episode artwork").performClick()
        composeRule.runOnIdle { assertEquals(1, openCount) }
    }

    @Test
    fun restoredPlaybackPositionIsVisibleInEpisodeRow() {
        composeRule.setContent {
            BoothAppTheme {
                EpisodeRow(
                    episode = episode().copy(durationMs = 600_000L, positionMs = 120_000L, inInbox = true),
                    onOpen = {},
                    onLongPress = {},
                    onPlay = {},
                    onTogglePlayPause = {},
                    active = false,
                    isPlaying = false,
                    onDownload = {},
                    downloadProgress = null,
                )
            }
        }

        // EpisodePlaybackButton labels a partially played episode with the remaining
        // time via formatPlaybackMinutes, so 600s - 120s renders as "8 min".
        composeRule.onNodeWithText("8 min").assertIsDisplayed()
    }

    private fun episode() = EpisodeEntity(
        id = 1L,
        podcastId = 1L,
        guid = "episode-1",
        title = "New episode",
        descriptionHtml = null,
        audioUrl = "https://example.com/episode.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = null,
        positionMs = 0L,
        completed = false,
        localUri = null,
        inInbox = false,
        firstSeenAtMillis = null,
    )
}
