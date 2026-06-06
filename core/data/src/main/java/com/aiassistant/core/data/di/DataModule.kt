package com.aiassistant.core.data.di

import android.content.Context
import com.aiassistant.core.data.datastore.SettingsDataStore
import com.aiassistant.core.data.repository.ChatRepositoryImpl
import com.aiassistant.core.data.repository.SettingsRepositoryImpl
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

    companion object {
        @Provides
        @Singleton
        fun provideSettingsDataStore(context: Context): SettingsDataStore {
            return SettingsDataStore(context)
        }
    }
}