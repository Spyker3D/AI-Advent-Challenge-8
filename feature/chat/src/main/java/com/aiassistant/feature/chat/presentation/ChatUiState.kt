package com.aiassistant.feature.chat.presentation

import com.aiassistant.core.domain.entity.AiModel
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.ContextStrategy
import com.aiassistant.core.domain.entity.StickyFacts
import com.aiassistant.core.domain.entity.ChatBranch
import com.aiassistant.core.domain.entity.Chat
import com.aiassistant.core.domain.memory.TaskContext
import com.aiassistant.core.domain.mcp.McpExecutionLogItem
import com.aiassistant.feature.chat.calendar.CalendarUiState

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val selectedModel: AiModel = AiModel.getDefault(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val systemPrompt: String = "You are a helpful AI assistant.",
    val invariantsEnabled: Boolean = true,
    val taskPipelineEnabled: Boolean = true,
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
    val lastSummaryMessageCount: Int = 0,
    // Context strategy fields
    val selectedContextStrategy: ContextStrategy = ContextStrategy.SLIDING_WINDOW,
    val stickyFacts: StickyFacts = StickyFacts(),
    val branches: List<ChatBranch> = emptyList(),
    val currentBranchId: String = "main",
    val factsStatus: String = "", // "Updating", "Updated", "Failed"
    // Multi-chat fields
    val chats: List<Chat> = emptyList(),
    val currentChatId: String = "main",
    val isChatDrawerOpen: Boolean = false,
    val activeTaskContext: TaskContext? = null,
    val mcpExecutionLogs: List<McpExecutionLogItem> = emptyList(),
    val isMcpExecutionVisible: Boolean = false,
    val ragEnabled: Boolean = false,
    val day23ImprovedRetrievalEnabled: Boolean = true,
    val ragSourcesByMessageId: Map<String, List<RagSourceUi>> = emptyMap(),
    val calendarState: CalendarUiState = CalendarUiState.Idle
)
