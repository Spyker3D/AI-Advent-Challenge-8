package com.aiassistant.feature.chat.presentation

data class VoiceInputUiState(
    val isListening: Boolean = false,
    val transcript: String? = null,
    val error: String? = null
)
