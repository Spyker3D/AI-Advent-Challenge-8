package com.aiassistant.core.data.di

import android.content.Context
import androidx.room.Room
import com.aiassistant.core.data.client.LlmClientImpl
import com.aiassistant.core.data.database.ChatDatabase
import com.aiassistant.core.data.database.ChatMessageDao
import com.aiassistant.core.data.datastore.SettingsDataStore
import com.aiassistant.core.data.mapper.ChatMessageMapper
import com.aiassistant.core.data.repository.ChatRepositoryImpl
import com.aiassistant.core.data.repository.SettingsRepositoryImpl
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.repository.ChatRepository
import com.aiassistant.core.domain.repository.SettingsRepository
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
    abstract fun bindLlmClient(
        llmClientImpl: LlmClientImpl
    ): LlmClient
    


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
            ).addMigrations(ChatDatabase.MIGRATION_1_2).build()
        }
        
        @Provides
        @Singleton
        fun provideChatMessageDao(database: ChatDatabase): ChatMessageDao {
            return database.chatMessageDao()
        }
        
        @Provides
        fun provideChatMessageMapper(): ChatMessageMapper {
            return ChatMessageMapper()
        }
    }
}
