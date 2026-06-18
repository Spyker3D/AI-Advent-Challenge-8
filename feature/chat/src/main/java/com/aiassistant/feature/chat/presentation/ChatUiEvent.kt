package com.aiassistant.feature.chat.presentation

import android.net.Uri
import com.aiassistant.core.domain.entity.ContextStrategy

sealed class ChatUiEvent {
    data class MessageChanged(val message: String) : ChatUiEvent()
    object SendMessage : ChatUiEvent()
    object ClearError : ChatUiEvent()
    object ClearChat : ChatUiEvent()
    
    // File attachment events
    data class FileAttached(val fileName: String, val fileContent: String) : ChatUiEvent()
    object ClearAttachedFile : ChatUiEvent()
    
    // Context compression events (handled through settings)
    object ClearSummary : ChatUiEvent()
    
    // Context strategy events
    data class ContextStrategySelected(val strategy: ContextStrategy) : ChatUiEvent()
    data class CreateBranch(val branchName: String) : ChatUiEvent()
    data class SwitchBranch(val branchId: String) : ChatUiEvent()
    data class DeleteBranch(val branchId: String) : ChatUiEvent()
    
    // Multi-chat events
    object NewChatClicked : ChatUiEvent()
    data class ChatSelected(val chatId: String) : ChatUiEvent()
    data class DeleteChatClicked(val chatId: String) : ChatUiEvent()
    object OpenChatDrawer : ChatUiEvent()
    object CloseChatDrawer : ChatUiEvent()

    object PauseTask : ChatUiEvent()
    object ResumeTask : ChatUiEvent()
    object ContinueTask : ChatUiEvent()
}
