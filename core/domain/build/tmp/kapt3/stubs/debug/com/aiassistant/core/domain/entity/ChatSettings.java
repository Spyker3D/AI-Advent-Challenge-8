package com.aiassistant.core.domain.entity;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0002\b\u0005\b\u0086\b\u0018\u0000 \u001d2\u00020\u0001:\u0001\u001dB-\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\t\u0010\u0013\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\tH\u00c6\u0003J1\u0010\u0017\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\tH\u00c6\u0001J\u0013\u0010\u0018\u001a\u00020\u00192\b\u0010\u001a\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001b\u001a\u00020\u0007H\u00d6\u0001J\t\u0010\u001c\u001a\u00020\tH\u00d6\u0001R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006\u001e"}, d2 = {"Lcom/aiassistant/core/domain/entity/ChatSettings;", "", "selectedModel", "Lcom/aiassistant/core/domain/entity/AiModel;", "temperature", "", "maxTokens", "", "systemPrompt", "", "(Lcom/aiassistant/core/domain/entity/AiModel;FILjava/lang/String;)V", "getMaxTokens", "()I", "getSelectedModel", "()Lcom/aiassistant/core/domain/entity/AiModel;", "getSystemPrompt", "()Ljava/lang/String;", "getTemperature", "()F", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "toString", "Companion", "domain_debug"})
public final class ChatSettings {
    @org.jetbrains.annotations.NotNull()
    private final com.aiassistant.core.domain.entity.AiModel selectedModel = null;
    private final float temperature = 0.0F;
    private final int maxTokens = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String systemPrompt = null;
    public static final float MIN_TEMPERATURE = 0.0F;
    public static final float MAX_TEMPERATURE = 2.0F;
    public static final int MIN_MAX_TOKENS = 10;
    public static final int MAX_MAX_TOKENS = 4000;
    @org.jetbrains.annotations.NotNull()
    public static final com.aiassistant.core.domain.entity.ChatSettings.Companion Companion = null;
    
    public ChatSettings(@org.jetbrains.annotations.NotNull()
    com.aiassistant.core.domain.entity.AiModel selectedModel, float temperature, int maxTokens, @org.jetbrains.annotations.NotNull()
    java.lang.String systemPrompt) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.aiassistant.core.domain.entity.AiModel getSelectedModel() {
        return null;
    }
    
    public final float getTemperature() {
        return 0.0F;
    }
    
    public final int getMaxTokens() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSystemPrompt() {
        return null;
    }
    
    public ChatSettings() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.aiassistant.core.domain.entity.AiModel component1() {
        return null;
    }
    
    public final float component2() {
        return 0.0F;
    }
    
    public final int component3() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.aiassistant.core.domain.entity.ChatSettings copy(@org.jetbrains.annotations.NotNull()
    com.aiassistant.core.domain.entity.AiModel selectedModel, float temperature, int maxTokens, @org.jetbrains.annotations.NotNull()
    java.lang.String systemPrompt) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0006X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/aiassistant/core/domain/entity/ChatSettings$Companion;", "", "()V", "MAX_MAX_TOKENS", "", "MAX_TEMPERATURE", "", "MIN_MAX_TOKENS", "MIN_TEMPERATURE", "domain_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}