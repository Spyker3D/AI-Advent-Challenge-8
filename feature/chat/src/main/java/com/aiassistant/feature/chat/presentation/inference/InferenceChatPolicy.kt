package com.aiassistant.feature.chat.presentation.inference

import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.inference.InferenceDebugMetadata
import com.aiassistant.core.domain.inference.InferenceMode
import com.aiassistant.feature.chat.calendar.CalendarUiState
import com.aiassistant.feature.chat.presentation.ChatUiState

internal fun inferenceRequestMode(
    provider: AiProvider,
    configuredMode: InferenceMode?
): InferenceMode? = configuredMode.takeIf { provider == AiProvider.LOCAL_OLLAMA }

internal fun ChatUiState.selectInferenceMode(mode: InferenceMode?): ChatUiState = copy(
    inferenceMode = mode.takeIf { provider == AiProvider.LOCAL_OLLAMA },
    routingEnabled = routingEnabled && (mode == null || provider != AiProvider.LOCAL_OLLAMA),
    microFirstEnabled = false
)

internal fun ChatUiState.selectMicroFirst(enabled: Boolean): ChatUiState = copy(
    microFirstEnabled = enabled && provider == AiProvider.LOCAL_OLLAMA,
    routingEnabled = routingEnabled && !enabled,
    inferenceMode = inferenceMode.takeUnless { enabled }
)

internal fun ChatUiState.toggleRouting(enabled: Boolean): ChatUiState = copy(
    routingEnabled = enabled,
    inferenceMode = inferenceMode.takeUnless { enabled },
    microFirstEnabled = microFirstEnabled && !enabled
)

internal fun ChatUiState.normalizeInferenceRouting(): ChatUiState = when {
    provider != AiProvider.LOCAL_OLLAMA -> copy(routingEnabled = false, inferenceMode = null, microFirstEnabled = false)
    routingEnabled -> copy(inferenceMode = null, microFirstEnabled = false)
    microFirstEnabled && inferenceMode != null -> copy(inferenceMode = null)
    else -> this
}

internal fun isInferenceSelectorVisible(state: ChatUiState): Boolean =
    state.provider == AiProvider.LOCAL_OLLAMA &&
        state.activeTaskContext == null &&
        !state.calendarState.blocksInferenceSelector() &&
        !state.isMcpExecutionActive

internal fun ownsCurrentMcpExecution(
    currentGeneration: Long,
    finishingGeneration: Long
): Boolean = currentGeneration == finishingGeneration

private fun CalendarUiState.blocksInferenceSelector(): Boolean = when (this) {
    CalendarUiState.Idle,
    is CalendarUiState.Success,
    is CalendarUiState.Error -> false
    is CalendarUiState.PermissionRequired,
    is CalendarUiState.PendingConfirmation,
    CalendarUiState.Executing -> true
}

internal fun Map<String, InferenceMode?>.withInferenceMode(
    assistantMessageId: String,
    mode: InferenceMode?
): Map<String, InferenceMode?> = this + (assistantMessageId to mode)

internal fun Map<String, InferenceDebugMetadata>.withInferenceMetadata(
    assistantMessageId: String,
    metadata: InferenceDebugMetadata
): Map<String, InferenceDebugMetadata> = this + (assistantMessageId to metadata)
