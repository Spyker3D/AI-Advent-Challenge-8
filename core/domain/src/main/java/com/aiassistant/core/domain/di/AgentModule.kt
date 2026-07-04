package com.aiassistant.core.domain.di

import com.aiassistant.core.domain.agent.ChatAgent
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.memory.MemoryOrchestrator
import com.aiassistant.core.domain.memory.PromptBuilder
import com.aiassistant.core.domain.memory.LlmTaskMemoryUpdater
import com.aiassistant.core.domain.memory.TaskMemoryUpdater
import com.aiassistant.core.domain.invariant.InvariantValidator
import com.aiassistant.core.domain.rag.LlmQueryRewriter
import com.aiassistant.core.domain.rag.QueryRewriter
import com.aiassistant.core.domain.repository.InvariantRepository
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
        invariantRepository: InvariantRepository,
        invariantValidator: InvariantValidator,
        ioDispatcher: CoroutineDispatcher
    ): ChatAgent {
        return ChatAgent(
            llmClient,
            memoryOrchestrator,
            promptBuilder,
            invariantRepository,
            invariantValidator,
            ioDispatcher
        )
    }
    
    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideQueryRewriter(
        llmQueryRewriter: LlmQueryRewriter
    ): QueryRewriter = llmQueryRewriter

    @Provides
    @Singleton
    fun provideTaskMemoryUpdater(
        llmTaskMemoryUpdater: LlmTaskMemoryUpdater
    ): TaskMemoryUpdater = llmTaskMemoryUpdater
}
