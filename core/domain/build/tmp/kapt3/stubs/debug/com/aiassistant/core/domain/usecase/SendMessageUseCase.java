package com.aiassistant.core.domain.usecase;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J$\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\u0006\u0010\b\u001a\u00020\tH\u0086B\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\n\u0010\u000bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\f"}, d2 = {"Lcom/aiassistant/core/domain/usecase/SendMessageUseCase;", "", "chatRepository", "Lcom/aiassistant/core/domain/repository/ChatRepository;", "(Lcom/aiassistant/core/domain/repository/ChatRepository;)V", "invoke", "Lkotlin/Result;", "", "chatRequest", "Lcom/aiassistant/core/domain/entity/ChatRequest;", "invoke-gIAlu-s", "(Lcom/aiassistant/core/domain/entity/ChatRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "domain_debug"})
public final class SendMessageUseCase {
    @org.jetbrains.annotations.NotNull()
    private final com.aiassistant.core.domain.repository.ChatRepository chatRepository = null;
    
    @javax.inject.Inject()
    public SendMessageUseCase(@org.jetbrains.annotations.NotNull()
    com.aiassistant.core.domain.repository.ChatRepository chatRepository) {
        super();
    }
}