package com.aiassistant.feature.chat.presentation.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClearChatConfirmationDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun displaysTitleAndActions() {
        showDialog()

        composeRule.onNodeWithText("Очистить историю сообщений?").assertIsDisplayed()
        composeRule.onNodeWithText("Отмена").assertIsDisplayed()
        composeRule.onNodeWithText("Очистить").assertIsDisplayed()
    }

    @Test
    fun cancelDismissesWithoutConfirming() {
        var dismissCount = 0
        var confirmCount = 0
        showDialog(
            onDismiss = { dismissCount++ },
            onConfirm = { confirmCount++ }
        )

        composeRule.onNodeWithText("Отмена").performClick()

        composeRule.onNodeWithText("Очистить историю сообщений?").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, dismissCount)
            assertEquals(0, confirmCount)
        }
    }

    @Test
    fun clearConfirmsWithoutDismissing() {
        var dismissCount = 0
        var confirmCount = 0
        showDialog(
            onDismiss = { dismissCount++ },
            onConfirm = { confirmCount++ }
        )

        composeRule.onNodeWithText("Очистить").performClick()

        composeRule.onNodeWithText("Очистить историю сообщений?").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(0, dismissCount)
            assertEquals(1, confirmCount)
        }
    }

    private fun showDialog(
        onDismiss: () -> Unit = {},
        onConfirm: () -> Unit = {}
    ) {
        composeRule.setContent {
            var isVisible by remember { mutableStateOf(true) }
            MaterialTheme {
                if (isVisible) {
                    ClearChatConfirmationDialog(
                        onDismiss = {
                            isVisible = false
                            onDismiss()
                        },
                        onConfirm = {
                            isVisible = false
                            onConfirm()
                        }
                    )
                }
            }
        }
    }
}
