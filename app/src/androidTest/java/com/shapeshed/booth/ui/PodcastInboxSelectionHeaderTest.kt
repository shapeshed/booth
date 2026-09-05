package com.shapeshed.booth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PodcastInboxSelectionHeaderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectAllSelectsEveryInboxEpisode() {
        var selectedIds = emptySet<Long>()
        composeRule.setContent {
            BoothAppTheme {
                PodcastInboxSelectionHeader(
                    inbox = listOf(episode(1L), episode(2L)),
                    selectedIds = selectedIds,
                    onSelectedIdsChange = { selectedIds = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Select all")
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertEquals(setOf(1L, 2L), selectedIds) }
    }

    private fun episode(id: Long) = EpisodeEntity(
        id = id,
        podcastId = 1L,
        guid = "episode-$id",
        title = "Episode $id",
        descriptionHtml = null,
        audioUrl = "https://example.com/$id.mp3",
        mimeType = "audio/mpeg",
        artworkUrl = null,
        publishedAtMillis = null,
        durationMs = null,
        positionMs = 0L,
        completed = false,
        localUri = null,
        inInbox = true,
        firstSeenAtMillis = null,
    )
}
