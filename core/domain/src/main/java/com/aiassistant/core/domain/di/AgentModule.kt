package com.aiassistant.core.domain.di

import com.aiassistant.core.domain.agent.ChatAgent
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.repository.ChatRepository
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
class AgentModule {
    
    @Provides
    @Singleton
    fun provideChatAgent(
        chatRepository: ChatRepository,
        llmClient: LlmClient,
        ioDispatcher: CoroutineDispatcher
    ): ChatAgent {
        return ChatAgent(chatRepository, llmClient, ioDispatcher)
    }
    
    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}