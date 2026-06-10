package com.aiassistant.feature.chat.presentation

import com.aiassistant.core.domain.entity.AiModel

sealed class ChatUiEvent {
    data class MessageChanged(val message: String) : ChatUiEvent()
    object SendMessage : ChatUiEvent()
    object ClearError : ChatUiEvent()
    object ClearChat : ChatUiEvent()
    
    // Model selection event
    data class ModelSelected(val model: AiModel) : ChatUiEvent()
}
