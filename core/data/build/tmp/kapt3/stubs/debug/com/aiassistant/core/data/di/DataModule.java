package com.aiassistant.core.data.di;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \u000b2\u00020\u0001:\u0001\u000bB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\'\u00a8\u0006\f"}, d2 = {"Lcom/aiassistant/core/data/di/DataModule;", "", "()V", "bindChatRepository", "Lcom/aiassistant/core/domain/repository/ChatRepository;", "chatRepositoryImpl", "Lcom/aiassistant/core/data/repository/ChatRepositoryImpl;", "bindSettingsRepository", "Lcom/aiassistant/core/domain/repository/SettingsRepository;", "settingsRepositoryImpl", "Lcom/aiassistant/core/data/repository/SettingsRepositoryImpl;", "Companion", "data_debug"})
public abstract class DataModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.aiassistant.core.data.di.DataModule.Companion Companion = null;
    
    public DataModule() {
        super();
    }
    
    @dagger.Binds()
    @org.jetbrains.annotations.NotNull()
    public abstract com.aiassistant.core.domain.repository.ChatRepository bindChatRepository(@org.jetbrains.annotations.NotNull()
    com.aiassistant.core.data.repository.ChatRepositoryImpl chatRepositoryImpl);
    
    @dagger.Binds()
    @org.jetbrains.annotations.NotNull()
    public abstract com.aiassistant.core.domain.repository.SettingsRepository bindSettingsRepository(@org.jetbrains.annotations.NotNull()
    com.aiassistant.core.data.repository.SettingsRepositoryImpl settingsRepositoryImpl);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u00a8\u0006\u0007"}, d2 = {"Lcom/aiassistant/core/data/di/DataModule$Companion;", "", "()V", "provideSettingsDataStore", "Lcom/aiassistant/core/data/datastore/SettingsDataStore;", "context", "Landroid/content/Context;", "data_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @dagger.Provides()
        @javax.inject.Singleton()
        @org.jetbrains.annotations.NotNull()
        public final com.aiassistant.core.data.datastore.SettingsDataStore provideSettingsDataStore(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
    }
}