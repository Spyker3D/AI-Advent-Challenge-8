package com.aiassistant.feature.chat.presentation.viewmodel

import com.aiassistant.feature.chat.voice.VoiceRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInputViewModelTest {
    @Test
    fun `successful recognition exposes transcript without sending`() {
        val recognizer = FakeVoiceRecognizer()
        val viewModel = VoiceInputViewModel(recognizer)

        viewModel.startListening()
        assertTrue(viewModel.uiState.value.isListening)

        recognizer.listener?.onResult("recognized text")

        assertFalse(viewModel.uiState.value.isListening)
        assertEquals("recognized text", viewModel.uiState.value.transcript)
        viewModel.consumeTranscript()
        assertNull(viewModel.uiState.value.transcript)
    }

    @Test
    fun `unavailable recognizer reports controlled error`() {
        val recognizer = FakeVoiceRecognizer(isAvailable = false)
        val viewModel = VoiceInputViewModel(recognizer)

        viewModel.startListening()

        assertFalse(viewModel.uiState.value.isListening)
        assertEquals("Speech recognition is not available on this device.", viewModel.uiState.value.error)
        assertNull(recognizer.listener)
    }

    @Test
    fun `recognition failure stops listening and reports error`() {
        val recognizer = FakeVoiceRecognizer()
        val viewModel = VoiceInputViewModel(recognizer)

        viewModel.startListening()
        recognizer.listener?.onError("Recognition failed")

        assertFalse(viewModel.uiState.value.isListening)
        assertEquals("Recognition failed", viewModel.uiState.value.error)
    }

    @Test
    fun `cancelling recognition resets listening state`() {
        val recognizer = FakeVoiceRecognizer()
        val viewModel = VoiceInputViewModel(recognizer)

        viewModel.startListening()
        viewModel.cancelListening()

        assertTrue(recognizer.cancelled)
        assertFalse(viewModel.uiState.value.isListening)
    }

    @Test
    fun `late result after cancellation is ignored`() {
        val recognizer = FakeVoiceRecognizer()
        val viewModel = VoiceInputViewModel(recognizer)

        viewModel.startListening()
        val cancelledListener = recognizer.listener
        viewModel.cancelListening()
        cancelledListener?.onResult("late result")

        assertNull(viewModel.uiState.value.transcript)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `release cancels and destroys recognizer`() {
        val recognizer = FakeVoiceRecognizer()
        val viewModel = VoiceInputViewModel(recognizer)

        viewModel.startListening()
        viewModel.release()

        assertTrue(recognizer.cancelled)
        assertTrue(recognizer.destroyed)
        assertFalse(viewModel.uiState.value.isListening)
    }

    @Test
    fun `permission denial reports actionable error`() {
        val viewModel = VoiceInputViewModel(FakeVoiceRecognizer())

        viewModel.onPermissionDenied(permanentlyDenied = true)

        assertEquals(
            "Microphone permission is disabled. Enable it in application settings.",
            viewModel.uiState.value.error
        )
    }

    private class FakeVoiceRecognizer(
        override val isAvailable: Boolean = true
    ) : VoiceRecognizer {
        var listener: VoiceRecognizer.Listener? = null
        var cancelled = false
        var destroyed = false

        override fun start(listener: VoiceRecognizer.Listener) {
            this.listener = listener
        }

        override fun cancel() {
            cancelled = true
        }

        override fun destroy() {
            destroyed = true
        }
    }
}
