package com.shapeshed.booth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.shapeshed.booth.ui.theme.BoothAppTheme
import com.shapeshed.booth.data.EpisodeEntity
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class PodcastHomeInboxContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyInboxExplainsWhereNewEpisodesAppear() {
        composeRule.setContent {
            BoothAppTheme {
                val inbox = flowOf(PagingData.empty<EpisodeEntity>()).collectAsLazyPagingItems()
                PodcastHomeInboxContent(
                    inbox = inbox,
                    podcastsById = emptyMap(),
                    playback = PlaybackUiState(),
                    inboxSelectionMode = false,
                    selectedInboxIds = emptySet(),
                    onSelectedInboxIdsChange = {},
                    onOpen = {},
                    onAddToQueue = {},
                    onDismiss = {},
                    onActions = {},
                    onPlay = {},
                    onDownload = {},
                    onRefresh = {},
                    refreshing = false,
                    downloadProgress = emptyMap(),
                )
            }
        }

        composeRule.onNodeWithText("Inbox is clear").assertIsDisplayed()
        composeRule.onNodeWithText("New episodes from your subscriptions will appear here.")
            .assertIsDisplayed()
    }
}
