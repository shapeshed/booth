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
 * Asserted with `assertExists`, not `assertIsDisplayed`. `BadgedBox` positions the dot as an overlay
 * offset out of the icon, and that geometry does not report as displayed in a bare test root even
 * with room made for it, while the same dot is clearly visible on a device inside a Scaffold. The
 * wiring is the thing under test, so existence is the honest assertion. Its on-screen appearance was
 * checked on a device.
 */
class PodcastHomeNavigationChromeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theDotIsShownWhenTheInboxHasUnseenEpisodes() {
        setContent(hasUnseenInboxEpisodes = true)

        composeRule.onNodeWithTag(INBOX_BADGE_TEST_TAG).assertExists()
    }

    @Test
    fun theDotIsAbsentWhenNothingIsUnseen() {
        setContent(hasUnseenInboxEpisodes = false)

        composeRule.onNodeWithTag(INBOX_BADGE_TEST_TAG).assertDoesNotExist()
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

        composeRule.onNodeWithTag(INBOX_BADGE_TEST_TAG).assertExists()
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
