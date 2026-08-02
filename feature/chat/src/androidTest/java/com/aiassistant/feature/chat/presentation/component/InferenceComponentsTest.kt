package com.aiassistant.feature.chat.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aiassistant.core.domain.inference.InferenceDebugMetadata
import com.aiassistant.core.domain.inference.InferenceMode
import com.aiassistant.core.domain.inference.InferenceStageMetadata
import com.aiassistant.core.domain.inference.StageStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InferenceComponentsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun selectorShowsThreeModesAndReportsSelection() {
        var selected: InferenceMode? = InferenceMode.MONOLITHIC
        composeRule.setContent {
            MaterialTheme {
                InferenceModeSelector(selected, { selected = it }, enabled = true)
            }
        }

        composeRule.onNodeWithText("Ordinary").assertIsDisplayed()
        composeRule.onNodeWithText("Monolithic").assertIsSelected()
        composeRule.onNodeWithText("Multi-stage").performClick()
        composeRule.runOnIdle { assertEquals(InferenceMode.MULTI_STAGE, selected) }
    }

    @Test
    fun selectorDisablesModeChangesWhileLoading() {
        composeRule.setContent {
            MaterialTheme {
                InferenceModeSelector(null, {}, enabled = false)
            }
        }

        composeRule.onNodeWithText("Ordinary").assertIsNotEnabled()
        composeRule.onNodeWithText("Monolithic").assertIsNotEnabled()
        composeRule.onNodeWithText("Multi-stage").assertIsNotEnabled()
    }

    @Test
    fun debugBlockShowsOrdinaryAndMultiStageDetails() {
        composeRule.setContent {
            MaterialTheme {
                InferenceDebugBlock(
                    mode = InferenceMode.MULTI_STAGE,
                    metadata = InferenceDebugMetadata(
                        mode = InferenceMode.MULTI_STAGE,
                        normalizedSummary = null,
                        decision = null,
                        stageMetadata = listOf(
                            InferenceStageMetadata("normalize", "qwen2.5:7b-instruct", 3, null, null, StageStatus.OK),
                            InferenceStageMetadata("decide", "qwen2.5:7b-instruct", 4, null, null, StageStatus.OK),
                            InferenceStageMetadata("present", "qwen2.5:7b-instruct", 5, null, null, StageStatus.OK)
                        ),
                        totalLatencyMs = 12,
                        totalModelCalls = 3,
                        formatCompliant = true
                    )
                )
            }
        }

        composeRule.onNodeWithText("Inference mode: Multi-stage").assertIsDisplayed()
        composeRule.onNodeWithText("Calls: 3").assertIsDisplayed()
        composeRule.onNodeWithText("Stage 1: normalize").assertIsDisplayed()
        composeRule.onNodeWithText("Stage 3: present").assertIsDisplayed()
    }

    @Test
    fun ordinaryDebugBlockHasExplicitMode() {
        composeRule.setContent { MaterialTheme { InferenceDebugBlock(null, null) } }

        composeRule.onNodeWithText("Inference mode: Ordinary").assertIsDisplayed()
    }
}
