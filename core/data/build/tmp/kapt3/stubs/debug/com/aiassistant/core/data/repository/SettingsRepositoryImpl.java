package com.aiassistant.core.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006H\u0016J\u0016\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0007H\u0096@\u00a2\u0006\u0002\u0010\u000bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/aiassistant/core/data/repository/SettingsRepositoryImpl;", "Lcom/aiassistant/core/domain/repository/SettingsRepository;", "settingsDataStore", "Lcom/aiassistant/core/data/datastore/SettingsDataStore;", "(Lcom/aiassistant/core/data/datastore/SettingsDataStore;)V", "getChatSettings", "Lkotlinx/coroutines/flow/Flow;", "Lcom/aiassistant/core/domain/entity/ChatSettings;", "saveChatSettings", "", "settings", "(Lcom/aiassistant/core/domain/entity/ChatSettings;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "data_debug"})
public final class SettingsRepositoryImpl implements com.aiassistant.core.domain.repository.SettingsRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.aiassistant.core.data.datastore.SettingsDataStore settingsDataStore = null;
    
    @javax.inject.Inject()
    public SettingsRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.aiassistant.core.data.datastore.SettingsDataStore settingsDataStore) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<com.aiassistant.core.domain.entity.ChatSettings> getChatSettings() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object saveChatSettings(@org.jetbrains.annotations.NotNull()
    com.aiassistant.core.domain.entity.ChatSettings settings, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}