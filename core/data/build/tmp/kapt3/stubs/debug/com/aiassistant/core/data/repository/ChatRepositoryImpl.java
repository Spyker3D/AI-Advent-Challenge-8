package com.aiassistant.core.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 \u00102\u00020\u0001:\u0001\u0010B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ$\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0006\u0010\f\u001a\u00020\rH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u000e\u0010\u000fR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u0011"}, d2 = {"Lcom/aiassistant/core/data/repository/ChatRepositoryImpl;", "Lcom/aiassistant/core/domain/repository/ChatRepository;", "openRouterApi", "Lcom/aiassistant/core/network/api/OpenRouterApi;", "chatMapper", "Lcom/aiassistant/core/data/mapper/ChatMapper;", "apiConfig", "Lcom/aiassistant/core/data/config/ApiConfig;", "(Lcom/aiassistant/core/network/api/OpenRouterApi;Lcom/aiassistant/core/data/mapper/ChatMapper;Lcom/aiassistant/core/data/config/ApiConfig;)V", "sendMessage", "Lkotlin/Result;", "", "chatRequest", "Lcom/aiassistant/core/domain/entity/ChatRequest;", "sendMessage-gIAlu-s", "(Lcom/aiassistant/core/domain/entity/ChatRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "data_debug"})
public final class ChatRepositoryImpl implements com.aiassistant.core.domain.repository.ChatRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.aiassistant.core.network.api.OpenRouterApi openRouterApi = null;
    @org.jetbrains.annotations.NotNull()
    private final com.aiassistant.core.data.mapper.ChatMapper chatMapper = null;
    @org.jetbrains.annotations.NotNull()
    private final com.aiassistant.core.data.config.ApiConfig apiConfig = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String BEARER_PREFIX = "Bearer ";
    @org.jetbrains.annotations.NotNull()
    public static final com.aiassistant.core.data.repository.ChatRepositoryImpl.Companion Companion = null;
    
    @javax.inject.Inject()
    public ChatRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.aiassistant.core.network.api.OpenRouterApi openRouterApi, @org.jetbrains.annotations.NotNull()
    com.aiassistant.core.data.mapper.ChatMapper chatMapper, @org.jetbrains.annotations.NotNull()
    com.aiassistant.core.data.config.ApiConfig apiConfig) {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/aiassistant/core/data/repository/ChatRepositoryImpl$Companion;", "", "()V", "BEARER_PREFIX", "", "data_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}