package com.aiassistant.feature.chat.presentation

import android.net.Uri
import com.aiassistant.core.domain.entity.AiModel

sealed class ChatUiEvent {
    data class MessageChanged(val message: String) : ChatUiEvent()
    object SendMessage : ChatUiEvent()
    object ClearError : ChatUiEvent()
    object ClearChat : ChatUiEvent()
    
    // Model selection event
    data class ModelSelected(val model: AiModel) : ChatUiEvent()
    
    // File attachment events
    data class FileAttached(val fileName: String, val fileContent: String) : ChatUiEvent()
    object ClearAttachedFile : ChatUiEvent()
}
