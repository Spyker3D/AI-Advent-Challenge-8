package com.aiassistant.feature.chat.presentation.inference

import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.inference.InferenceDebugMetadata
import com.aiassistant.core.domain.inference.InferenceMode
import com.aiassistant.core.domain.memory.TaskContext
import com.aiassistant.feature.chat.presentation.ChatUiState
import com.aiassistant.feature.chat.calendar.CalendarEventDraft
import com.aiassistant.feature.chat.calendar.CalendarUiState
import com.aiassistant.feature.chat.calendar.PendingCalendarAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ChatViewModelInferencePolicyTest {
    @Test
    fun `mode is disabled by default and external providers bypass inference`() {
        assertNull(inferenceRequestMode(AiProvider.LOCAL_OLLAMA, null))
        assertNull(inferenceRequestMode(AiProvider.OPENAI, InferenceMode.MONOLITHIC))
        assertNull(inferenceRequestMode(AiProvider.PRIVATE_VPS, InferenceMode.MULTI_STAGE))
    }

    @Test
    fun `local provider snapshots configured inference mode`() {
        assertEquals(
            InferenceMode.MULTI_STAGE,
            inferenceRequestMode(AiProvider.LOCAL_OLLAMA, InferenceMode.MULTI_STAGE)
        )
    }

    @Test
    fun `debug metadata is associated with exact assistant message id`() {
        val metadata = InferenceDebugMetadata(
            mode = InferenceMode.MONOLITHIC,
            normalizedSummary = null,
            decision = null,
            stageMetadata = emptyList(),
            totalLatencyMs = 1,
            totalModelCalls = 1,
            formatCompliant = true
        )

        val result = emptyMap<String, InferenceDebugMetadata>()
            .withInferenceMetadata("assistant-42", metadata)

        assertEquals(setOf("assistant-42"), result.keys)
        assertSame(metadata, result["assistant-42"])
    }

    @Test
    fun `selecting inference disables routing and enabling routing selects ordinary`() {
        val routed = ChatUiState(provider = AiProvider.LOCAL_OLLAMA, routingEnabled = true)

        val monolithic = routed.selectInferenceMode(InferenceMode.MONOLITHIC)
        assertEquals(InferenceMode.MONOLITHIC, monolithic.inferenceMode)
        assertEquals(false, monolithic.routingEnabled)

        val multiStage = routed.selectInferenceMode(InferenceMode.MULTI_STAGE)
        assertEquals(InferenceMode.MULTI_STAGE, multiStage.inferenceMode)
        assertEquals(false, multiStage.routingEnabled)

        val routingEnabled = monolithic.toggleRouting(true)
        assertEquals(true, routingEnabled.routingEnabled)
        assertNull(routingEnabled.inferenceMode)
    }

    @Test
    fun `impossible state is normalized to routing with ordinary inference`() {
        val normalized = ChatUiState(
            provider = AiProvider.LOCAL_OLLAMA,
            routingEnabled = true,
            inferenceMode = InferenceMode.MULTI_STAGE
        ).normalizeInferenceRouting()

        assertEquals(true, normalized.routingEnabled)
        assertNull(normalized.inferenceMode)
    }

    @Test
    fun `selector is visible only in local ordinary chat context`() {
        val local = ChatUiState(provider = AiProvider.LOCAL_OLLAMA)
        assertEquals(true, isInferenceSelectorVisible(local))
        assertEquals(false, isInferenceSelectorVisible(local.copy(provider = AiProvider.OPENAI)))
        assertEquals(false, isInferenceSelectorVisible(local.copy(isMcpExecutionActive = true)))
        assertEquals(
            false,
            isInferenceSelectorVisible(
                local.copy(activeTaskContext = TaskContext("task", "Task", "Description"))
            )
        )
    }

    @Test
    fun `external provider rejects inference selection without carrying it to local`() {
        val external = ChatUiState(provider = AiProvider.OPENAI)
            .selectInferenceMode(InferenceMode.MONOLITHIC)

        assertNull(external.inferenceMode)
        assertNull(external.copy(provider = AiProvider.LOCAL_OLLAMA).inferenceMode)
    }

    @Test
    fun `calendar terminal states allow selector while active states hide it`() {
        val local = ChatUiState(provider = AiProvider.LOCAL_OLLAMA)
        val draft = CalendarEventDraft("Title", 1, 2, "UTC", null)
        val pending = PendingCalendarAction.CreateEvent("action", draft)

        assertEquals(false, isInferenceSelectorVisible(local.copy(calendarState = CalendarUiState.PendingConfirmation(pending))))
        assertEquals(false, isInferenceSelectorVisible(local.copy(calendarState = CalendarUiState.Executing)))
        assertEquals(true, isInferenceSelectorVisible(local.copy(calendarState = CalendarUiState.Success("done"))))
        assertEquals(true, isInferenceSelectorVisible(local.copy(calendarState = CalendarUiState.Error("failed"))))
    }

    @Test
    fun `ordinary mode marker is associated with exact assistant message id`() {
        val result = emptyMap<String, InferenceMode?>().withInferenceMode("assistant-7", null)

        assertEquals(setOf("assistant-7"), result.keys)
        assertNull(result["assistant-7"])
    }

    @Test
    fun `stale MCP completion cannot finish a newer execution`() {
        assertEquals(false, ownsCurrentMcpExecution(currentGeneration = 2, finishingGeneration = 1))
        assertEquals(true, ownsCurrentMcpExecution(currentGeneration = 2, finishingGeneration = 2))
    }
}
