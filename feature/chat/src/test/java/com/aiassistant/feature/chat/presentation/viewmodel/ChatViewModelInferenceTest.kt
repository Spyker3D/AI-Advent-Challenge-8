package com.aiassistant.feature.chat.presentation.viewmodel

import com.aiassistant.core.domain.agent.ChatAgent
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.Chat
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.inference.InferenceDebugMetadata
import com.aiassistant.core.domain.inference.InferenceExecutionResult
import com.aiassistant.core.domain.inference.InferenceMode
import com.aiassistant.core.domain.mcp.McpOrchestratorAgent
import com.aiassistant.core.domain.mcp.McpPipelineAgent
import com.aiassistant.core.domain.mcp.McpPipelineResult
import com.aiassistant.core.domain.memory.TaskMemoryMerger
import com.aiassistant.core.domain.memory.TaskMemoryUpdater
import com.aiassistant.core.domain.memory.TaskPipelineOrchestrator
import com.aiassistant.core.domain.rag.QueryRewriter
import com.aiassistant.core.domain.rag.RagEmbeddingClient
import com.aiassistant.core.domain.rag.RagIndexLoader
import com.aiassistant.core.domain.rag.RagPromptBuilder
import com.aiassistant.core.domain.rag.RagRetriever
import com.aiassistant.core.domain.repository.ChatRepository
import com.aiassistant.core.domain.repository.WorkingMemoryRepository
import com.aiassistant.core.domain.usecase.ClearChatHistoryUseCase
import com.aiassistant.core.domain.usecase.GetChatHistoryUseCase
import com.aiassistant.core.domain.usecase.GetChatSettingsUseCase
import com.aiassistant.core.domain.usecase.RunInferenceUseCase
import com.aiassistant.core.domain.usecase.RunMicroFirstInferenceUseCase
import com.aiassistant.core.domain.microfirst.MicroFirstResult
import com.aiassistant.core.domain.usecase.SaveChatSettingsUseCase
import com.aiassistant.core.domain.usecase.SendMessageUseCase
import com.aiassistant.feature.chat.calendar.CalendarAssistantService
import com.aiassistant.feature.chat.presentation.ChatUiEvent
import com.aiassistant.feature.chat.voice.VoiceInputCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelInferenceTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `default disabled uses ordinary chat agent`() = runTest(dispatcher) {
        val fixture = fixture(AiProvider.LOCAL_OLLAMA)
        whenever(fixture.chatAgent.sendMessage(any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.success(AiChatResponse("ordinary", null)))

        fixture.send("hello")
        advanceUntilIdle()

        verify(fixture.chatAgent).sendMessage(any(), any(), anyOrNull(), anyOrNull())
        verify(fixture.inference, never()).invoke(any(), any())
        val assistant = fixture.viewModel.uiState.value.messages.single { it.role == MessageRole.ASSISTANT }
        assertTrue(fixture.viewModel.uiState.value.inferenceModeByMessageId.containsKey(assistant.id))
        assertNull(fixture.viewModel.uiState.value.inferenceModeByMessageId[assistant.id])
    }

    @Test
    fun `inference selection and routing events maintain invariant`() = runTest(dispatcher) {
        val fixture = fixture(AiProvider.LOCAL_OLLAMA)

        fixture.viewModel.handleEvent(ChatUiEvent.RoutingToggled(true))
        fixture.viewModel.handleEvent(ChatUiEvent.InferenceModeSelected(InferenceMode.MONOLITHIC))
        assertFalse(fixture.viewModel.uiState.value.routingEnabled)
        assertEquals(InferenceMode.MONOLITHIC, fixture.viewModel.uiState.value.inferenceMode)

        fixture.viewModel.handleEvent(ChatUiEvent.InferenceModeSelected(InferenceMode.MULTI_STAGE))
        assertFalse(fixture.viewModel.uiState.value.routingEnabled)
        assertEquals(InferenceMode.MULTI_STAGE, fixture.viewModel.uiState.value.inferenceMode)

        fixture.viewModel.handleEvent(ChatUiEvent.RoutingToggled(true))
        assertTrue(fixture.viewModel.uiState.value.routingEnabled)
        assertNull(fixture.viewModel.uiState.value.inferenceMode)

        fixture.viewModel.handleEvent(ChatUiEvent.MicroFirstToggled(true))
        assertTrue(fixture.viewModel.uiState.value.microFirstEnabled)
        assertFalse(fixture.viewModel.uiState.value.routingEnabled)
        assertNull(fixture.viewModel.uiState.value.inferenceMode)

        fixture.viewModel.handleEvent(ChatUiEvent.RoutingToggled(true))
        assertFalse(fixture.viewModel.uiState.value.microFirstEnabled)
    }

    @Test
    fun `micro first uses current input only and binds result to persisted assistant`() = runTest(dispatcher) {
        val fixture = fixture(AiProvider.LOCAL_OLLAMA)
        fixture.viewModel.handleEvent(ChatUiEvent.MicroFirstToggled(true))
        fixture.viewModel.handleEvent(ChatUiEvent.FileAttached("note.txt", "attachment"))
        val result = microFirstResult(fallback = false)
        whenever(fixture.microFirst.invoke(any())).thenReturn(Result.success(result))

        fixture.send("incident")
        advanceUntilIdle()

        verify(fixture.microFirst).invoke("incident")
        verify(fixture.chatAgent, never()).sendMessage(any(), any(), anyOrNull(), anyOrNull())
        verify(fixture.inference, never()).invoke(any(), any())
        val captor = argumentCaptor<Message>()
        verify(fixture.repository, times(2)).saveMessage(captor.capture(), any())
        val assistant = captor.allValues.single { it.role == MessageRole.ASSISTANT }
        assertEquals(result, fixture.viewModel.uiState.value.microFirstResultByMessageId[assistant.id])
    }

    @Test
    fun `micro first fallback result is returned without ordinary agent`() = runTest(dispatcher) {
        val fixture = fixture(AiProvider.LOCAL_OLLAMA)
        fixture.viewModel.handleEvent(ChatUiEvent.MicroFirstToggled(true))
        whenever(fixture.microFirst.invoke(any())).thenReturn(Result.success(microFirstResult(fallback = true)))
        fixture.send("incident")
        advanceUntilIdle()
        assertEquals("fallback", fixture.viewModel.uiState.value.messages.last().content)
        verify(fixture.chatAgent, never()).sendMessage(any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `multi stage selection invokes exact inference mode`() = runTest(dispatcher) {
        val fixture = fixture(AiProvider.LOCAL_OLLAMA)
        fixture.viewModel.handleEvent(ChatUiEvent.InferenceModeSelected(InferenceMode.MULTI_STAGE))
        whenever(fixture.inference.invoke(any(), any())).thenReturn(
            Result.success(inferenceResult(InferenceMode.MULTI_STAGE))
        )

        fixture.send("incident")
        advanceUntilIdle()

        verify(fixture.inference).invoke("incident", InferenceMode.MULTI_STAGE)
        verify(fixture.chatAgent, never()).sendMessage(any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `local inference saves one user and assistant and binds metadata to assistant id`() = runTest(dispatcher) {
        val fixture = fixture(AiProvider.LOCAL_OLLAMA)
        fixture.viewModel.setInferenceMode(InferenceMode.MONOLITHIC)
        whenever(fixture.inference.invoke(any(), any())).thenReturn(Result.success(inferenceResult()))

        fixture.send("incident")
        advanceUntilIdle()

        val captor = argumentCaptor<Message>()
        verify(fixture.repository, times(2)).saveMessage(captor.capture(), any())
        assertEquals(listOf(MessageRole.USER, MessageRole.ASSISTANT), captor.allValues.map { it.role })
        val assistant = captor.allValues.single { it.role == MessageRole.ASSISTANT }
        assertEquals(setOf(assistant.id), fixture.viewModel.uiState.value.inferenceDebugByMessageId.keys)
        verify(fixture.inference).invoke("incident", InferenceMode.MONOLITHIC)
    }

    @Test
    fun `external providers bypass inference`() = runTest(dispatcher) {
        listOf(AiProvider.OPENAI, AiProvider.PRIVATE_VPS).forEach { provider ->
            val fixture = fixture(provider)
            fixture.viewModel.setInferenceMode(InferenceMode.MONOLITHIC)
            assertNull(fixture.viewModel.uiState.value.inferenceMode)
            fixture.viewModel.handleEvent(ChatUiEvent.MicroFirstToggled(true))
            assertFalse(fixture.viewModel.uiState.value.microFirstEnabled)
            whenever(fixture.chatAgent.sendMessage(any(), any(), anyOrNull(), anyOrNull()))
                .thenReturn(Result.success(AiChatResponse("ordinary", null)))
            fixture.send("hello")
            advanceUntilIdle()
            verify(fixture.inference, never()).invoke(any(), any())
            verify(fixture.microFirst, never()).invoke(any())
            verify(fixture.chatAgent).sendMessage(any(), any(), anyOrNull(), anyOrNull())
        }
    }

    @Test
    fun `MCP completion and failure restore selector eligibility`() = runTest(dispatcher) {
        listOf(false, true).forEach { fails ->
            val fixture = fixture(AiProvider.LOCAL_OLLAMA, mcpPipelineHandles = true)
            fixture.viewModel.handleEvent(ChatUiEvent.MicroFirstToggled(true))
            if (fails) {
                whenever(fixture.mcpPipeline.run(any())).thenThrow(IllegalStateException("failed"))
            } else {
                val result = McpPipelineResult("weather", emptyList(), "sunny")
                whenever(fixture.mcpPipeline.run(any())).thenReturn(result)
                whenever(fixture.mcpPipeline.formatChatAnswer(result)).thenReturn("sunny")
            }

            fixture.send("weather")
            advanceUntilIdle()

            verify(fixture.microFirst, never()).invoke(any())

            assertFalse(fixture.viewModel.uiState.value.isMcpExecutionActive)
            assertTrue(
                com.aiassistant.feature.chat.presentation.inference.isInferenceSelectorVisible(
                    fixture.viewModel.uiState.value
                )
            )
        }
    }

    @Test
    fun `inference failure saves no assistant and resets loading with error`() = runTest(dispatcher) {
        val fixture = fixture(AiProvider.LOCAL_OLLAMA)
        fixture.viewModel.setInferenceMode(InferenceMode.MONOLITHIC)
        whenever(fixture.inference.invoke(any(), any())).thenReturn(Result.failure(IllegalStateException("failed")))

        fixture.send("incident")
        advanceUntilIdle()

        val captor = argumentCaptor<Message>()
        verify(fixture.repository).saveMessage(captor.capture(), any())
        assertEquals(MessageRole.USER, captor.firstValue.role)
        assertFalse(fixture.viewModel.uiState.value.isLoading)
        assertEquals("failed", fixture.viewModel.uiState.value.error)
    }

    @Test
    fun `cancellation is not exposed and resets loading`() = runTest(dispatcher) {
        val fixture = fixture(AiProvider.LOCAL_OLLAMA)
        fixture.viewModel.setInferenceMode(InferenceMode.MONOLITHIC)
        whenever(fixture.inference.invoke(any(), any())).thenReturn(Result.failure(CancellationException("cancel")))

        fixture.send("incident")
        advanceUntilIdle()

        assertFalse(fixture.viewModel.uiState.value.isLoading)
        assertNull(fixture.viewModel.uiState.value.error)
    }

    @Test
    fun `duplicate send while active is ignored`() = runTest(dispatcher) {
        val fixture = fixture(AiProvider.LOCAL_OLLAMA)
        fixture.viewModel.setInferenceMode(InferenceMode.MONOLITHIC)
        whenever(fixture.inference.invoke(any(), any())).thenReturn(Result.success(inferenceResult()))

        fixture.send("incident")
        fixture.viewModel.handleEvent(ChatUiEvent.MessageChanged("duplicate"))
        fixture.viewModel.handleEvent(ChatUiEvent.SendMessage)
        runCurrent()

        verify(fixture.inference, times(1)).invoke(any(), any())
        advanceUntilIdle()
    }

    @Test
    fun `chat switch keeps completed metadata without appending to new visible chat`() = runTest(dispatcher) {
        val fixture = fixture(AiProvider.LOCAL_OLLAMA)
        fixture.viewModel.setInferenceMode(InferenceMode.MONOLITHIC)
        whenever(fixture.inference.invoke(any(), any())).thenReturn(Result.success(inferenceResult()))
        whenever(fixture.history.invoke("chat-b")).thenReturn(emptyList())

        fixture.send("incident")
        fixture.viewModel.handleEvent(ChatUiEvent.ChatSelected("chat-b"))
        runCurrent()
        advanceUntilIdle()

        val captor = argumentCaptor<Message>()
        verify(fixture.repository, times(2)).saveMessage(captor.capture(), any())
        val assistant = captor.allValues.single { it.role == MessageRole.ASSISTANT }
        assertTrue(fixture.viewModel.uiState.value.messages.none { it.id == assistant.id })
        assertEquals(setOf(assistant.id), fixture.viewModel.uiState.value.inferenceDebugByMessageId.keys)
    }

    private suspend fun TestScope.fixture(
        provider: AiProvider,
        mcpPipelineHandles: Boolean = false
    ): Fixture {
        val repository = mock<ChatRepository>()
        val chatAgent = mock<ChatAgent>()
        val inference = mock<RunInferenceUseCase>()
        val microFirst = mock<RunMicroFirstInferenceUseCase>()
        val settings = mock<GetChatSettingsUseCase>()
        val history = mock<GetChatHistoryUseCase>()
        val chatA = Chat("chat-a", "A", 0, 0)
        val chatB = Chat("chat-b", "B", 0, 0)
        whenever(settings.invoke()).thenReturn(flowOf(ChatSettings(provider = provider, taskPipelineEnabled = false)))
        whenever(repository.getChats()).thenReturn(listOf(chatA, chatB))
        whenever(history.invoke("chat-a")).thenReturn(emptyList())

        val mcpPipeline = mock<McpPipelineAgent> {
            on { canHandleWeatherPipeline(any()) }.thenReturn(mcpPipelineHandles)
        }
        val viewModel = ChatViewModel(
            mock<SendMessageUseCase>(), settings, mock<SaveChatSettingsUseCase>(), history,
            mock<ClearChatHistoryUseCase>(), chatAgent, repository, mock<LlmClient>(),
            mock<TaskPipelineOrchestrator>(), mock<WorkingMemoryRepository>(), mock<McpOrchestratorAgent> {
                on { canHandleOrchestration(any()) }.thenReturn(false)
            }, mcpPipeline,
            mock<RagIndexLoader>(), mock<RagEmbeddingClient>(), mock<RagRetriever>(), mock<RagPromptBuilder>(),
            mock<QueryRewriter>(), mock<TaskMemoryUpdater>(), mock<TaskMemoryMerger>(),
            mock<CalendarAssistantService> { on { canHandle(any()) }.thenReturn(false) },
            mock<VoiceInputCoordinator>(), inference, microFirst
        )
        advanceUntilIdle()
        return Fixture(viewModel, repository, chatAgent, inference, microFirst, history, mcpPipeline)
    }

    private fun Fixture.send(text: String) {
        viewModel.handleEvent(ChatUiEvent.MessageChanged(text))
        viewModel.handleEvent(ChatUiEvent.SendMessage)
    }

    private fun inferenceResult(mode: InferenceMode = InferenceMode.MONOLITHIC) = InferenceExecutionResult(
        "inference",
        InferenceDebugMetadata(mode, null, null, emptyList(), 1, 1, true)
    )

    private fun microFirstResult(fallback: Boolean) = MicroFirstResult(
        finalText = if (fallback) "fallback" else "micro",
        handledByMicro = !fallback,
        fallbackUsed = fallback,
        microResult = null,
        fallbackModel = if (fallback) "large" else null,
        microLatencyMs = 1,
        fallbackLatencyMs = if (fallback) 2 else null,
        totalLatencyMs = if (fallback) 3 else 1,
        largeLlmCalls = if (fallback) 1 else 0,
        fallbackReason = null
    )

    private data class Fixture(
        val viewModel: ChatViewModel,
        val repository: ChatRepository,
        val chatAgent: ChatAgent,
        val inference: RunInferenceUseCase,
        val microFirst: RunMicroFirstInferenceUseCase,
        val history: GetChatHistoryUseCase,
        val mcpPipeline: McpPipelineAgent
    )
}
