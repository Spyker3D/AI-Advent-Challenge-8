package com.aiassistant.core.data.di

import android.content.Context
import androidx.room.Room
import com.aiassistant.core.data.client.LlmClientImpl
import com.aiassistant.core.data.database.ChatDatabase
import com.aiassistant.core.data.database.ChatMessageDao
import com.aiassistant.core.data.datastore.SettingsDataStore
import com.aiassistant.core.data.mapper.ChatMessageMapper
import com.aiassistant.core.data.mcp.McpAgentRepositoryImpl
import com.aiassistant.core.data.rag.AndroidOllamaEmbeddingClient
import com.aiassistant.core.data.rag.AndroidRagIndexLoader
import com.aiassistant.core.data.repository.ChatRepositoryImpl
import com.aiassistant.core.data.repository.InvariantRepositoryImpl
import com.aiassistant.core.data.repository.LongTermMemoryRepositoryImpl
import com.aiassistant.core.data.repository.SettingsRepositoryImpl
import com.aiassistant.core.data.repository.WorkingMemoryRepositoryImpl
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.mcp.McpAgentRepository
import com.aiassistant.core.domain.rag.RagEmbeddingClient
import com.aiassistant.core.domain.rag.RagIndexLoader
import com.aiassistant.core.domain.repository.ChatRepository
import com.aiassistant.core.domain.repository.InvariantRepository
import com.aiassistant.core.domain.repository.LongTermMemoryRepository
import com.aiassistant.core.domain.repository.SettingsRepository
import com.aiassistant.core.domain.repository.WorkingMemoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
abstract class DataModule {

    @Binds
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    abstract fun bindWorkingMemoryRepository(
        workingMemoryRepositoryImpl: WorkingMemoryRepositoryImpl
    ): WorkingMemoryRepository

    @Binds
    abstract fun bindLongTermMemoryRepository(
        longTermMemoryRepositoryImpl: LongTermMemoryRepositoryImpl
    ): LongTermMemoryRepository

    @Binds
    abstract fun bindInvariantRepository(
        invariantRepositoryImpl: InvariantRepositoryImpl
    ): InvariantRepository

    @Binds
    abstract fun bindMcpAgentRepository(
        impl: McpAgentRepositoryImpl
    ): McpAgentRepository
    
    @Binds
    abstract fun bindLlmClient(
        llmClientImpl: LlmClientImpl
    ): LlmClient

    @Binds
    abstract fun bindRagIndexLoader(
        androidRagIndexLoader: AndroidRagIndexLoader
    ): RagIndexLoader

    @Binds
    abstract fun bindRagEmbeddingClient(
        androidOllamaEmbeddingClient: AndroidOllamaEmbeddingClient
    ): RagEmbeddingClient


    companion object {
        @Provides
        @Singleton
        fun provideSettingsDataStore(context: Context): SettingsDataStore {
            return SettingsDataStore(context)
        }
        
        @Provides
        @Singleton
        fun provideChatDatabase(context: Context): ChatDatabase {
            return Room.databaseBuilder(
                context,
                ChatDatabase::class.java,
                "chat_database"
            ).addMigrations(
                ChatDatabase.MIGRATION_1_2,
                ChatDatabase.MIGRATION_2_3,
                ChatDatabase.MIGRATION_3_4,
                ChatDatabase.MIGRATION_4_5
            ).build()
        }
        
        @Provides
        @Singleton
        fun provideChatMessageDao(database: ChatDatabase): ChatMessageDao {
            return database.chatMessageDao()
        }
        
        @Provides
        @Singleton
        fun provideChatDao(database: ChatDatabase): com.aiassistant.core.data.database.dao.ChatDao {
            return database.chatDao()
        }
        
        @Provides
        fun provideChatMessageMapper(): ChatMessageMapper {
            return ChatMessageMapper()
        }
    }
}
