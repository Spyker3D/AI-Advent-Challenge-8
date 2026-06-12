package com.aiassistant.feature.chat.presentation

import android.net.Uri

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
}
