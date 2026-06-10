package com.aiassistant.core.domain.di

import com.aiassistant.core.domain.agent.ChatAgent
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
        chatRepository: com.aiassistant.core.domain.repository.ChatRepository,
        llmClient: com.aiassistant.core.domain.agent.LlmClient
    ): ChatAgent {
        return ChatAgent(chatRepository, llmClient)
    }
    
    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}