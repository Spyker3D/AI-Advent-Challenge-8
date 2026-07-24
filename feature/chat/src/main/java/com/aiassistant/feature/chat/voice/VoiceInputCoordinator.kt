package com.aiassistant.feature.chat.voice

import javax.inject.Inject

class VoiceInputCoordinator(
    private val gateway: SpeechRecognitionGateway
) {
    @Inject
    constructor(gateway: AndroidSpeechRecognitionGateway) : this(
        gateway as SpeechRecognitionGateway
    )
    private var generation = 0L
    private var operation = 0L
    private var active = false
    private var stopping = false
    private var released = false
    private var partial = ""
    private var observer: Observer? = null

    fun attach(observer: Observer) {
        this.observer = observer
    }

    fun start() {
        if (released || active) return
        if (!gateway.isAvailable) {
            observer?.onState(
                VoiceInputState.Guidance("Voice recognition is unavailable on this device.")
            )
            return
        }
        generation++
        active = true
        stopping = false
        partial = ""
        startSegment(generation)
    }

    fun stop() {
        if (!active || stopping || released) return
        stopping = true
        observer?.onState(VoiceInputState.Stopping(partial))
        gateway.stop().onFailure {
            finishWithError("Voice input could not be stopped.")
        }
    }

    fun cancel() {
        if (released) return
        generation++
        operation++
        active = false
        stopping = false
        partial = ""
        gateway.cancel()
        observer?.onState(VoiceInputState.Idle)
    }

    fun release() {
        if (released) return
        generation++
        operation++
        active = false
        stopping = false
        partial = ""
        released = true
        gateway.cancel()
        gateway.release()
        observer = null
    }

    private fun startSegment(sessionGeneration: Long) {
        if (!isCurrent(sessionGeneration)) return
        val segmentOperation = ++operation
        partial = ""
        observer?.onState(VoiceInputState.Listening())
        gateway.start(
            object : SpeechRecognitionGateway.Listener {
                override fun onPartial(text: String) {
                    if (!isCurrent(sessionGeneration, segmentOperation)) return
                    partial = text
                    observer?.onState(
                        if (stopping) VoiceInputState.Stopping(text)
                        else VoiceInputState.Listening(text)
                    )
                }

                override fun onFinal(text: String) {
                    if (!isCurrent(sessionGeneration, segmentOperation)) return
                    partial = ""
                    if (text.isNotBlank()) observer?.onFinalText(text)
                    if (stopping) {
                        finish()
                    } else {
                        startSegment(sessionGeneration)
                    }
                }

                override fun onError(error: VoiceRecognitionError) {
                    if (!isCurrent(sessionGeneration, segmentOperation)) return
                    if (stopping) {
                        finish()
                    } else if (error == VoiceRecognitionError.RecoverableSegmentEnd) {
                        startSegment(sessionGeneration)
                    } else {
                        val message = (error as VoiceRecognitionError.Controlled).message
                        finishWithError(message)
                    }
                }
            }
        ).onFailure {
            finishWithError(it.message ?: "Voice recognition could not start.")
        }
    }

    private fun finish() {
        active = false
        stopping = false
        partial = ""
        operation++
        observer?.onState(VoiceInputState.Idle)
    }

    private fun finishWithError(message: String) {
        generation++
        operation++
        active = false
        stopping = false
        partial = ""
        gateway.cancel()
        observer?.onState(VoiceInputState.Guidance(message))
    }

    private fun isCurrent(sessionGeneration: Long, segmentOperation: Long? = null): Boolean =
        !released &&
            active &&
            generation == sessionGeneration &&
            (segmentOperation == null || operation == segmentOperation)

    interface Observer {
        fun onState(state: VoiceInputState)
        fun onFinalText(text: String)
    }
}
