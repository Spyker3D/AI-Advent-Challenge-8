package com.aiassistant.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.aiassistant.feature.chat.presentation.VoiceInputUiState
import com.aiassistant.feature.chat.voice.VoiceRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class VoiceInputViewModel @Inject constructor(
    private val voiceRecognizer: VoiceRecognizer
) : ViewModel() {
    private val _uiState = MutableStateFlow(VoiceInputUiState())
    val uiState: StateFlow<VoiceInputUiState> = _uiState.asStateFlow()
    private var nextSessionId = 0L
    private var activeSessionId: Long? = null

    fun startListening() {
        if (_uiState.value.isListening) return
        if (!voiceRecognizer.isAvailable) {
            _uiState.value = _uiState.value.copy(
                error = "Speech recognition is not available on this device."
            )
            return
        }
        val sessionId = ++nextSessionId
        activeSessionId = sessionId
        _uiState.value = _uiState.value.copy(isListening = true, error = null)
        voiceRecognizer.start(object : VoiceRecognizer.Listener {
            override fun onResult(transcript: String) {
                handleResult(sessionId, transcript)
            }

            override fun onError(message: String) {
                handleError(sessionId, message)
            }
        })
    }

    fun cancelListening() {
        activeSessionId = null
        voiceRecognizer.cancel()
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    fun onPermissionDenied(permanentlyDenied: Boolean) {
        _uiState.value = _uiState.value.copy(
            isListening = false,
            error = if (permanentlyDenied) {
                "Microphone permission is disabled. Enable it in application settings."
            } else {
                "Microphone permission is required for voice input."
            }
        )
    }

    fun release() {
        activeSessionId = null
        voiceRecognizer.cancel()
        voiceRecognizer.destroy()
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    private fun handleResult(sessionId: Long, transcript: String) {
        if (activeSessionId != sessionId) return
        activeSessionId = null
        _uiState.value = _uiState.value.copy(
            isListening = false,
            transcript = transcript,
            error = null
        )
    }

    private fun handleError(sessionId: Long, message: String) {
        if (activeSessionId != sessionId) return
        activeSessionId = null
        _uiState.value = _uiState.value.copy(isListening = false, error = message)
    }

    fun consumeTranscript() {
        _uiState.value = _uiState.value.copy(transcript = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        release()
    }
}
