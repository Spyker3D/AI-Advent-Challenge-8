package com.aiassistant.core.domain.usecase

import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ClearConversationSummaryUseCaseTest {

    @Test
    fun `clears persisted conversation summary`() = runBlocking {
        val repository = FakeSettingsRepository(ChatSettings(conversationSummary = "Existing summary"))
        val useCase = ClearConversationSummaryUseCase(repository)

        useCase()

        assertEquals("", repository.settings.value.conversationSummary)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun `does not persist when summary is already empty`() = runBlocking {
        val repository = FakeSettingsRepository(ChatSettings())
        val useCase = ClearConversationSummaryUseCase(repository)

        useCase()

        assertEquals(0, repository.saveCount)
    }

    private class FakeSettingsRepository(initial: ChatSettings) : SettingsRepository {
        val settings = MutableStateFlow(initial)
        var saveCount = 0

        override fun getChatSettings(): Flow<ChatSettings> = settings

        override suspend fun saveChatSettings(settings: ChatSettings) {
            saveCount += 1
            this.settings.value = settings
        }
    }
}