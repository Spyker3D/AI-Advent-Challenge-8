package com.aiassistant.feature.chat.presentation

import com.aiassistant.core.domain.entity.AiModel
import com.aiassistant.core.domain.entity.Message

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val selectedModel: AiModel = AiModel.getDefault(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val systemPrompt: String = "You are a helpful AI assistant.",
    val error: String? = null,
    val currentMessage: String = "",
    // Day 2 fields (loaded from settings)
    val useJsonFormat: Boolean = false,
    val limitLength: Boolean = false,
    val useStopSequence: Boolean = false,
    val stopSequenceText: String = "",
    // File attachment fields
    val attachedFileName: String? = null,
    val attachedFileText: String? = null,
    // Context compression fields
    val useContextCompression: Boolean = false,
    val keepLastMessagesCount: Int = 6,
    val conversationSummary: String = "",
    val summaryMessageCount: Int = 0,
    val fullHistoryTokensEstimate: Int = 0,
    val compressedHistoryTokensEstimate: Int = 0,
    val savedTokensEstimate: Int = 0,
    val compressionRatioPercent: Int = 0,
    val lastSummaryMessageCount: Int = 0
)
