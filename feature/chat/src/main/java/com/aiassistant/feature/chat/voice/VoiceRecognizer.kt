package com.aiassistant.feature.chat.voice

interface VoiceRecognizer {
    val isAvailable: Boolean

    fun start(listener: Listener)

    fun cancel()

    fun destroy()

    interface Listener {
        fun onResult(transcript: String)
        fun onError(message: String)
    }
}

fun mergeVoiceTranscript(currentMessage: String, transcript: String): String {
    val recognizedText = transcript.trim()
    if (recognizedText.isEmpty()) return currentMessage
    if (currentMessage.isBlank()) return recognizedText
    return currentMessage.trimEnd() + " " + recognizedText
}
