package com.aiassistant.feature.chat.voice

sealed interface VoiceInputState {
    object Idle : VoiceInputState
    object PermissionRequired : VoiceInputState
    data class Listening(val partialText: String = "") : VoiceInputState
    data class Stopping(val partialText: String = "") : VoiceInputState
    data class Guidance(
        val message: String,
        val openSettings: Boolean = false
    ) : VoiceInputState
}

sealed interface VoiceRecognitionError {
    object RecoverableSegmentEnd : VoiceRecognitionError
    data class Controlled(val message: String) : VoiceRecognitionError
}

interface SpeechRecognitionGateway {
    val isAvailable: Boolean

    fun start(listener: Listener): Result<Unit>
    fun stop(): Result<Unit>
    fun cancel()
    fun release()

    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(error: VoiceRecognitionError)
    }
}

object VoiceDraftMerger {
    fun merge(existing: String, recognized: String): String {
        val normalizedRecognized = recognized.trim().replace(Regex("\\s+"), " ")
        if (normalizedRecognized.isEmpty()) return existing
        val normalizedExisting = existing.trimEnd()
        return if (normalizedExisting.isEmpty()) {
            normalizedRecognized
        } else {
            "$normalizedExisting $normalizedRecognized"
        }
    }
}
