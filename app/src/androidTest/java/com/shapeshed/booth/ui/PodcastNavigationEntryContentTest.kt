package com.shapeshed.booth.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Rule
import org.junit.Test

class PodcastNavigationEntryContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun retainedSubscriptionsEntryShowsImportedPodcasts() {
        val podcasts = mutableStateOf(emptyList<String>())

        composeRule.setContent {
            BoothAppTheme {
                val importedPodcast = podcasts.value.singleOrNull()
                val currentContent = rememberUpdatedState<@Composable () -> Unit>(
                    newValue = {
                        if (importedPodcast == null) {
                            Text("Start listening")
                        } else {
                            Text(importedPodcast)
                        }
                    },
                )
                val backStack = rememberNavBackStack(PodcastNavigationKey.Subscriptions)
                NavDisplay(
                    backStack = backStack,
                    onBack = {},
                    entryProvider = entryProvider {
                        entry<PodcastNavigationKey.Subscriptions> { currentContent.value() }
                    },
                )
            }
        }

        composeRule.onNodeWithText("Start listening").assertIsDisplayed()

        composeRule.runOnIdle {
            podcasts.value = listOf("Imported podcast")
        }

        composeRule.onNodeWithText("Imported podcast").assertIsDisplayed()
    }
}
