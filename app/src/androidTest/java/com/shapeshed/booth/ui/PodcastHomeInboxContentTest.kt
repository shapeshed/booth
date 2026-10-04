package com.shapeshed.booth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.shapeshed.booth.data.EpisodeEntity
import com.shapeshed.booth.ui.theme.BoothAppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class PodcastHomeInboxContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyInboxExplainsWhereNewEpisodesAppear() {
        showInbox(flowOf(emptyPage(LoadState.NotLoading(endOfPaginationReached = true))))

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Inbox is clear").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Inbox is clear").assertIsDisplayed()
        composeRule.onNodeWithText("New episodes from your subscriptions will appear here.")
            .assertIsDisplayed()
    }

    @Test
    fun loadingInboxDoesNotClaimToBeEmptyBeforeTheQueryCompletes() {
        val pages = MutableStateFlow(emptyPage(LoadState.Loading))
        showInbox(pages)

        composeRule.onNodeWithText("Inbox is clear").assertDoesNotExist()
        composeRule.runOnIdle { pages.value = emptyPage(LoadState.NotLoading(endOfPaginationReached = true)) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Inbox is clear").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Inbox is clear").assertIsDisplayed()
    }

    @Test
    fun loadingInboxRevealsEpisodesWithoutAnEmptyMessage() {
        val pages = MutableStateFlow(emptyPage(LoadState.Loading))
        showInbox(pages)

        composeRule.onNodeWithText("Inbox is clear").assertDoesNotExist()
        composeRule.runOnIdle {
            pages.value = PagingData.from(listOf(episode()))
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Loaded inbox episode").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Loaded inbox episode").assertIsDisplayed()
        composeRule.onNodeWithText("Inbox is clear").assertDoesNotExist()
    }

    @Test
    fun failedInboxLoadDoesNotClaimToBeEmpty() {
        showInbox(flowOf(emptyPage(LoadState.Error(IllegalStateException("Test load failed")))))

        composeRule.onNodeWithText("Inbox is clear").assertDoesNotExist()
    }

    private fun emptyPage(refresh: LoadState): PagingData<EpisodeEntity> = PagingData.empty(
        sourceLoadStates = LoadStates(
            refresh = refresh,
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true),
        ),
    )

    private fun episode() = EpisodeEntity(
        id = 1L,
        podcastId = 1L,
        guid = "loaded-episode",
        title = "Loaded inbox episode",
        descriptionHtml = null,
        audioUrl = "https://example.com/episode.mp3",
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

    private fun showInbox(pages: Flow<PagingData<EpisodeEntity>>) {
        composeRule.setContent {
            BoothAppTheme {
                val inbox = pages.collectAsLazyPagingItems()
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
                    playbackProgress = PlaybackProgress(),
                )
            }
        }
    }
}
