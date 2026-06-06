package com.aiassistant.feature.settings.presentation.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011J\b\u0010\u0012\u001a\u00020\u000fH\u0002J\b\u0010\u0013\u001a\u00020\u000fH\u0002J\b\u0010\u0014\u001a\u00020\u000fH\u0002J\u001c\u0010\u0015\u001a\u00020\u000f2\u0012\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u00180\u0017H\u0002R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0019"}, d2 = {"Lcom/aiassistant/feature/settings/presentation/viewmodel/SettingsViewModel;", "Landroidx/lifecycle/ViewModel;", "getChatSettingsUseCase", "Lcom/aiassistant/core/domain/usecase/GetChatSettingsUseCase;", "saveChatSettingsUseCase", "Lcom/aiassistant/core/domain/usecase/SaveChatSettingsUseCase;", "(Lcom/aiassistant/core/domain/usecase/GetChatSettingsUseCase;Lcom/aiassistant/core/domain/usecase/SaveChatSettingsUseCase;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "handleEvent", "", "event", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent;", "observeChatSettings", "resetToDefaults", "saveSettings", "updateSettings", "update", "Lkotlin/Function1;", "Lcom/aiassistant/core/domain/entity/ChatSettings;", "settings_debug"})
public final class SettingsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.aiassistant.core.domain.usecase.GetChatSettingsUseCase getChatSettingsUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final com.aiassistant.core.domain.usecase.SaveChatSettingsUseCase saveChatSettingsUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.aiassistant.feature.settings.presentation.SettingsUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.aiassistant.feature.settings.presentation.SettingsUiState> uiState = null;
    
    @javax.inject.Inject()
    public SettingsViewModel(@org.jetbrains.annotations.NotNull()
    com.aiassistant.core.domain.usecase.GetChatSettingsUseCase getChatSettingsUseCase, @org.jetbrains.annotations.NotNull()
    com.aiassistant.core.domain.usecase.SaveChatSettingsUseCase saveChatSettingsUseCase) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.aiassistant.feature.settings.presentation.SettingsUiState> getUiState() {
        return null;
    }
    
    private final void observeChatSettings() {
    }
    
    public final void handleEvent(@org.jetbrains.annotations.NotNull()
    com.aiassistant.feature.settings.presentation.SettingsUiEvent event) {
    }
    
    private final void updateSettings(kotlin.jvm.functions.Function1<? super com.aiassistant.core.domain.entity.ChatSettings, com.aiassistant.core.domain.entity.ChatSettings> update) {
    }
    
    private final void saveSettings() {
    }
    
    private final void resetToDefaults() {
    }
}