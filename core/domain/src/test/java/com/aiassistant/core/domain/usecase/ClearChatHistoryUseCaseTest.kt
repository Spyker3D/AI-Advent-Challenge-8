package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.Chat
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.FormattedAiResponse
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.repository.ChatRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ClearChatHistoryUseCaseTest {

    @Test
    fun `clears only the selected branch`() = runBlocking {
        val repository = RecordingChatRepository()
        val useCase = ClearChatHistoryUseCase(repository)

        useCase("feature-branch")

        assertEquals(listOf("feature-branch"), repository.clearedBranchIds)
    }

    @Test
    fun `clears main branch by default`() = runBlocking {
        val repository = RecordingChatRepository()
        val useCase = ClearChatHistoryUseCase(repository)

        useCase()

        assertEquals(listOf("main"), repository.clearedBranchIds)
    }

    private class RecordingChatRepository : ChatRepository {
        val clearedBranchIds = mutableListOf<String>()

        override suspend fun clearMessages(branchId: String) {
            clearedBranchIds += branchId
        }

        override suspend fun sendMessage(chatRequest: ChatRequest): Result<AiChatResponse> = unsupported()
        override suspend fun sendMessageWithRestrictions(chatRequest: ChatRequest, useJsonFormat: Boolean, limitLength: Boolean, useStopSequence: Boolean, stopSequenceText: String): Result<AiChatResponse> = unsupported()
        override fun parseFormattedResponse(response: String): Result<FormattedAiResponse> = unsupported()
        override suspend fun saveMessage(message: Message, branchId: String) = Unit
        override suspend fun getMessages(branchId: String): List<Message> = emptyList()
        override suspend fun getChats(): List<Chat> = emptyList()
        override suspend fun createChat(title: String): Chat = unsupported()
        override suspend fun deleteChat(chatId: String) = Unit
        override suspend fun updateChatMeta(chatId: String, title: String, preview: String) = Unit
        override suspend fun updateChatActiveTaskContext(chatId: String, taskContextId: String?) = Unit

        private fun <T> unsupported(): T = error("Not used by this test")
    }
}