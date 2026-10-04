package com.shapeshed.booth.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Rule
import org.junit.Test

/**
 * The Inbox dot, at the level the user sees it.
 *
 * The rule and the query are covered elsewhere ([com.shapeshed.booth.data.InboxBadgeTest] and
 * `InboxBadgeDeviceTest`), but neither reaches the chrome. If the condition here were inverted, or the
 * flag never wired through, every one of those tests would still pass and the dot would simply never
 * appear.
 *
 * The dot is a decorative `Badge` with no text and no semantics, so it carries a test tag that is
 * added only when it is drawn. Its presence or absence is therefore the assertion.
 *
 * Found in the unmerged tree on purpose. The badge is a decorative `Badge` inside a `BadgedBox`
 * inside a navigation item, and the navigation item merges the semantics of its children, so the
 * merged tree has no node carrying this tag — CI reported exactly that ("the unmerged tree contains
 * '1' node that matches"). Looking in the unmerged tree is what finds the dot itself rather than the
 * merged "Inbox" item.
 *
 * `assertExists` rather than `assertIsDisplayed`, because what is under test is the wiring: that the
 * flag reaches the chrome and the dot is composed at all. Its appearance is pinned by the screenshot
 * tests instead, which is the right tool for it.
 */
class PodcastHomeNavigationChromeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theDotIsShownWhenTheInboxHasUnseenEpisodes() {
        setContent(hasUnseenInboxEpisodes = true)

        composeRule.onNodeWithTag(INBOX_BADGE_TEST_TAG, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theDotIsAbsentWhenNothingIsUnseen() {
        setContent(hasUnseenInboxEpisodes = false)

        composeRule.onNodeWithTag(INBOX_BADGE_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    /**
     * The dot marks the Inbox specifically, and is visible while another tab is selected. That is the
     * only situation it is meant to appear in: you cannot see an unseen Inbox from inside the Inbox.
     */
    @Test
    fun theDotMarksTheInboxTabRatherThanTheSelectedOne() {
        composeRule.setContent {
            BoothAppTheme {
                PodcastHomeBottomNavigation(
                    visible = true,
                    selectedTab = PodcastTab.UP_NEXT,
                    hasUnseenInboxEpisodes = true,
                    onSelectTab = {},
                )
            }
        }

        composeRule.onNodeWithTag(INBOX_BADGE_TEST_TAG, useUnmergedTree = true).assertExists()
    }

    private fun setContent(hasUnseenInboxEpisodes: Boolean) {
        composeRule.setContent {
            BoothAppTheme {
                PodcastHomeBottomNavigation(
                    visible = true,
                    selectedTab = PodcastTab.HOME,
                    hasUnseenInboxEpisodes = hasUnseenInboxEpisodes,
                    onSelectTab = {},
                )
            }
        }
    }
}
