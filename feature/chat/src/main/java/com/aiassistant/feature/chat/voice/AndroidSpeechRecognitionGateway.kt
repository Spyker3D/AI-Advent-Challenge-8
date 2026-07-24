package com.aiassistant.feature.chat.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import javax.inject.Inject

class AndroidSpeechRecognitionGateway @Inject constructor(
    context: Context
) : SpeechRecognitionGateway {
    private val applicationContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var released = false

    override val isAvailable: Boolean
        get() = !released && SpeechRecognizer.isRecognitionAvailable(applicationContext)

    override fun start(listener: SpeechRecognitionGateway.Listener): Result<Unit> =
        onMainThread {
            check(!released) { "Voice recognition has been released." }
            if (!SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
                error("Voice recognition is unavailable on this device.")
            }
            val activeRecognizer = recognizer ?: SpeechRecognizer
                .createSpeechRecognizer(applicationContext)
                .also { recognizer = it }
            activeRecognizer.setRecognitionListener(AndroidRecognitionListener(listener))
            activeRecognizer.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
            )
        }

    override fun stop(): Result<Unit> = onMainThread {
        recognizer?.stopListening() ?: error("Voice recognition is not active.")
    }

    override fun cancel() {
        onMainThread { recognizer?.cancel() }
    }

    override fun release() {
        onMainThread {
            if (!released) {
                released = true
                recognizer?.cancel()
                recognizer?.destroy()
                recognizer = null
            }
        }
    }

    private fun <T> onMainThread(action: () -> T): Result<T> = runCatching {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "SpeechRecognizer operations must run on the main thread."
        }
        action()
    }

    private class AndroidRecognitionListener(
        private val listener: SpeechRecognitionGateway.Listener
    ) : RecognitionListener {
        override fun onPartialResults(partialResults: Bundle?) {
            bestText(partialResults)?.let(listener::onPartial)
        }

        override fun onResults(results: Bundle?) {
            listener.onFinal(bestText(results).orEmpty())
        }

        override fun onError(error: Int) {
            listener.onError(
                if (
                    error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                ) {
                    VoiceRecognitionError.RecoverableSegmentEnd
                } else {
                    VoiceRecognitionError.Controlled(errorMessage(error))
                }
            )
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        private fun bestText(bundle: Bundle?): String? =
            bundle
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()

        private fun errorMessage(error: Int): String = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Voice input failed because of an audio error."
            SpeechRecognizer.ERROR_CLIENT -> "Voice input was interrupted."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission is required for voice input."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice recognition network error."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognition is busy."
            SpeechRecognizer.ERROR_SERVER -> "Voice recognition service failed."
            else -> "Voice recognition failed."
        }
    }
}
