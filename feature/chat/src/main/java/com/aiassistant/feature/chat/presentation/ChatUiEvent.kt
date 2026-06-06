package com.aiassistant.feature.chat.presentation

sealed class ChatUiEvent {
    data class MessageChanged(val message: String) : ChatUiEvent()
    object SendMessage : ChatUiEvent()
    object ClearError : ChatUiEvent()
    object ClearChat : ChatUiEvent()
}