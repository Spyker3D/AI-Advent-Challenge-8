package com.aiassistant.core.domain.di

import com.aiassistant.core.domain.agent.ChatAgent
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.memory.MemoryOrchestrator
import com.aiassistant.core.domain.memory.PromptBuilder
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
        llmClient: LlmClient,
        memoryOrchestrator: MemoryOrchestrator,
        promptBuilder: PromptBuilder,
        ioDispatcher: CoroutineDispatcher
    ): ChatAgent {
        return ChatAgent(llmClient, memoryOrchestrator, promptBuilder, ioDispatcher)
    }
    
    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
