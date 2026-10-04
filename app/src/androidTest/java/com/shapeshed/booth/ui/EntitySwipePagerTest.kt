package com.shapeshed.booth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The shared pager, driven through the composable rather than its index helpers.
 *
 * `PodcastDetailSwipePagerTest` and `PodcastEpisodeSwipePagerTest` cover only the pure index
 * functions, so nothing rendered the pager itself. That left the single-item path unverified, which
 * is the path this pager changed: it used to branch to a `Box` and now runs through a one-page
 * `HorizontalPager`. A single item is not a rare case here, it is every detail opened from search or
 * now playing, so a regression in it would be user-visible immediately.
 *
 * The layout assertion is the point of the last test. The old comment on this code warned that the
 * modifier has to reach the container or the content wrap-sizes inside its pane, and moving the
 * single-item path onto the pager is exactly the change that could break it.
 */
class EntitySwipePagerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private data class Item(val id: Long, val label: String)

    private fun items(vararg labels: String) =
        labels.mapIndexed { index, label -> Item(id = index + 1L, label = label) }

    @Test
    fun aSingleItemRendersItsContent() {
        composeRule.setContent {
            BoothAppTheme {
                EntitySwipePager(
                    items = items("only"),
                    selectedId = 1L,
                    key = Item::id,
                    onSelect = {},
                    modifier = Modifier.fillMaxSize(),
                ) { item ->
                    Text(item.label)
                }
            }
        }

        composeRule.onNodeWithText("only").assertIsDisplayed()
    }

    @Test
    fun multipleItemsRenderTheSelectedOne() {
        composeRule.setContent {
            BoothAppTheme {
                EntitySwipePager(
                    items = items("first", "second", "third"),
                    selectedId = 2L,
                    key = Item::id,
                    onSelect = {},
                    modifier = Modifier.fillMaxSize(),
                ) { item ->
                    Text(item.label)
                }
            }
        }

        composeRule.onNodeWithText("second").assertIsDisplayed()
    }

    @Test
    fun anEmptyListRendersNothingRatherThanCrashing() {
        composeRule.setContent {
            BoothAppTheme {
                EntitySwipePager(
                    items = emptyList<Item>(),
                    selectedId = 1L,
                    key = Item::id,
                    onSelect = {},
                    modifier = Modifier.fillMaxSize(),
                ) { item ->
                    Text(item.label)
                }
            }
        }

        composeRule.onNodeWithText("first").assertDoesNotExist()
    }

    /**
     * The container has to fill its parent, including when there is one item.
     *
     * A pager that wrap-sized instead would make the list inside it collapse to its content width,
     * which is the failure the old comment described. Measured against the 300dp parent rather than
     * asserted on a description, because a wrap-sized container still renders its content.
     */
    @Test
    fun aSingleItemStillFillsItsParent() {
        composeRule.setContent {
            BoothAppTheme {
                Box(Modifier.size(300.dp)) {
                    EntitySwipePager(
                        items = items("only"),
                        selectedId = 1L,
                        key = Item::id,
                        onSelect = {},
                        modifier = Modifier.fillMaxSize(),
                    ) { item ->
                        Box(Modifier.fillMaxSize().testTag("page-${item.id}")) {
                            Text(item.label)
                        }
                    }
                }
            }
        }

        val expected = with(composeRule.density) { 300.dp.toPx() }
        val actual = composeRule.onNodeWithTag("page-1").fetchSemanticsNode().boundsInRoot.width
        assertEquals("the single-item pager did not fill its parent", expected, actual, 1f)
    }

    @Test
    fun multipleItemsAlsoFillTheParent() {
        composeRule.setContent {
            BoothAppTheme {
                Box(Modifier.size(300.dp)) {
                    EntitySwipePager(
                        items = items("first", "second"),
                        selectedId = 1L,
                        key = Item::id,
                        onSelect = {},
                        modifier = Modifier.fillMaxSize(),
                    ) { item ->
                        Box(Modifier.fillMaxSize().testTag("page-${item.id}")) {
                            Text(item.label)
                        }
                    }
                }
            }
        }

        val expected = with(composeRule.density) { 300.dp.toPx() }
        val actual = composeRule.onNodeWithTag("page-1").fetchSemanticsNode().boundsInRoot.width
        assertEquals("the multi-item pager did not fill its parent", expected, actual, 1f)
    }
}
