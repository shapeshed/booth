package com.shapeshed.booth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shapeshed.booth.data.SleepTimerState
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SleepTimerSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingPresetReportsDurationAndDismisses() {
        var selectedDuration = 0L
        var dismissed = false
        composeRule.setContent {
            BoothAppTheme {
                SleepTimerSheet(
                    active = null,
                    onSet = { selectedDuration = it },
                    onCancel = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithText("15 min").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(15 * 60_000L, selectedDuration)
            assertEquals(true, dismissed)
        }
    }

    @Test
    fun activeTimerCanBeCancelled() {
        var cancelled = false
        composeRule.setContent {
            BoothAppTheme {
                SleepTimerSheet(
                    active = SleepTimerState(totalMs = 15 * 60_000L, remainingMs = 10 * 60_000L),
                    onSet = {},
                    onCancel = { cancelled = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(true, cancelled) }
    }
}
