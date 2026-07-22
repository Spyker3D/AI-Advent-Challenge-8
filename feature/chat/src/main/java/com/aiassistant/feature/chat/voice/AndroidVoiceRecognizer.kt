package com.aiassistant.feature.chat.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import javax.inject.Inject

class AndroidVoiceRecognizer @Inject constructor(
    context: Context
) : VoiceRecognizer {
    private val applicationContext = context.applicationContext
    private var speechRecognizer: SpeechRecognizer? = null
    private var cancellationPending = false

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(applicationContext)

    override fun start(listener: VoiceRecognizer.Listener) {
        cancellationPending = false
        if (!isAvailable) {
            listener.onError("Speech recognition is not available on this device.")
            return
        }

        val recognizer = speechRecognizer ?: SpeechRecognizer
            .createSpeechRecognizer(applicationContext)
            .also { speechRecognizer = it }
        recognizer.setRecognitionListener(listener.toRecognitionListener())
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
        )
    }

    override fun cancel() {
        cancellationPending = true
        speechRecognizer?.cancel()
    }

    override fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun VoiceRecognizer.Listener.toRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onResults(results: Bundle?) {
            if (cancellationPending) {
                cancellationPending = false
                return
            }
            val transcript = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (transcript.isBlank()) {
                this@toRecognitionListener.onError("No speech was recognized. Please try again.")
            } else {
                this@toRecognitionListener.onResult(transcript)
            }
        }

        override fun onError(error: Int) {
            if (cancellationPending) {
                cancellationPending = false
                return
            }
            this@toRecognitionListener.onError(error.toUserMessage())
        }
    }

    private fun Int.toUserMessage(): String = when (this) {
        SpeechRecognizer.ERROR_AUDIO -> "The microphone could not capture audio."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice input."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition could not reach the network."
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognized. Please try again."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Please try again."
        SpeechRecognizer.ERROR_SERVER -> "The speech recognition service failed. Please try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected. Please try again."
        else -> "Speech recognition failed. Please try again."
    }
}
