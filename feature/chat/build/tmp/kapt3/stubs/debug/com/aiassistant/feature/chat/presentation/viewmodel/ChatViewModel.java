package com.aiassistant.feature.chat.presentation.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011J\b\u0010\u0012\u001a\u00020\u000fH\u0002J\b\u0010\u0013\u001a\u00020\u000fH\u0002R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0014"}, d2 = {"Lcom/aiassistant/feature/chat/presentation/viewmodel/ChatViewModel;", "Landroidx/lifecycle/ViewModel;", "sendMessageUseCase", "Lcom/aiassistant/core/domain/usecase/SendMessageUseCase;", "getChatSettingsUseCase", "Lcom/aiassistant/core/domain/usecase/GetChatSettingsUseCase;", "(Lcom/aiassistant/core/domain/usecase/SendMessageUseCase;Lcom/aiassistant/core/domain/usecase/GetChatSettingsUseCase;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/aiassistant/feature/chat/presentation/ChatUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "handleEvent", "", "event", "Lcom/aiassistant/feature/chat/presentation/ChatUiEvent;", "observeChatSettings", "sendMessage", "chat_debug"})
public final class ChatViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.aiassistant.core.domain.usecase.SendMessageUseCase sendMessageUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final com.aiassistant.core.domain.usecase.GetChatSettingsUseCase getChatSettingsUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.aiassistant.feature.chat.presentation.ChatUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.aiassistant.feature.chat.presentation.ChatUiState> uiState = null;
    
    @javax.inject.Inject()
    public ChatViewModel(@org.jetbrains.annotations.NotNull()
    com.aiassistant.core.domain.usecase.SendMessageUseCase sendMessageUseCase, @org.jetbrains.annotations.NotNull()
    com.aiassistant.core.domain.usecase.GetChatSettingsUseCase getChatSettingsUseCase) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.aiassistant.feature.chat.presentation.ChatUiState> getUiState() {
        return null;
    }
    
    private final void observeChatSettings() {
    }
    
    public final void handleEvent(@org.jetbrains.annotations.NotNull()
    com.aiassistant.feature.chat.presentation.ChatUiEvent event) {
    }
    
    private final void sendMessage() {
    }
}