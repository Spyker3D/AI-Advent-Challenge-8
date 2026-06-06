package com.aiassistant.core.network.dto;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B+\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\u0002\u0010\tJ\u000f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\u000b\u0010\u0011\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\u000b\u0010\u0012\u001a\u0004\u0018\u00010\bH\u00c6\u0003J1\u0010\u0013\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00062\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\bH\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0017\u001a\u00020\u0018H\u00d6\u0001J\t\u0010\u0019\u001a\u00020\u001aH\u00d6\u0001R\u001c\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0018\u0010\u0007\u001a\u0004\u0018\u00010\b8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0018\u0010\u0005\u001a\u0004\u0018\u00010\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006\u001b"}, d2 = {"Lcom/aiassistant/core/network/dto/ChatResponseDto;", "", "choices", "", "Lcom/aiassistant/core/network/dto/ChoiceDto;", "usage", "Lcom/aiassistant/core/network/dto/UsageDto;", "error", "Lcom/aiassistant/core/network/dto/ErrorDto;", "(Ljava/util/List;Lcom/aiassistant/core/network/dto/UsageDto;Lcom/aiassistant/core/network/dto/ErrorDto;)V", "getChoices", "()Ljava/util/List;", "getError", "()Lcom/aiassistant/core/network/dto/ErrorDto;", "getUsage", "()Lcom/aiassistant/core/network/dto/UsageDto;", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "", "network_debug"})
public final class ChatResponseDto {
    @com.google.gson.annotations.SerializedName(value = "choices")
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.aiassistant.core.network.dto.ChoiceDto> choices = null;
    @com.google.gson.annotations.SerializedName(value = "usage")
    @org.jetbrains.annotations.Nullable()
    private final com.aiassistant.core.network.dto.UsageDto usage = null;
    @com.google.gson.annotations.SerializedName(value = "error")
    @org.jetbrains.annotations.Nullable()
    private final com.aiassistant.core.network.dto.ErrorDto error = null;
    
    public ChatResponseDto(@org.jetbrains.annotations.NotNull()
    java.util.List<com.aiassistant.core.network.dto.ChoiceDto> choices, @org.jetbrains.annotations.Nullable()
    com.aiassistant.core.network.dto.UsageDto usage, @org.jetbrains.annotations.Nullable()
    com.aiassistant.core.network.dto.ErrorDto error) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.aiassistant.core.network.dto.ChoiceDto> getChoices() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.aiassistant.core.network.dto.UsageDto getUsage() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.aiassistant.core.network.dto.ErrorDto getError() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.aiassistant.core.network.dto.ChoiceDto> component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.aiassistant.core.network.dto.UsageDto component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.aiassistant.core.network.dto.ErrorDto component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.aiassistant.core.network.dto.ChatResponseDto copy(@org.jetbrains.annotations.NotNull()
    java.util.List<com.aiassistant.core.network.dto.ChoiceDto> choices, @org.jetbrains.annotations.Nullable()
    com.aiassistant.core.network.dto.UsageDto usage, @org.jetbrains.annotations.Nullable()
    com.aiassistant.core.network.dto.ErrorDto error) {
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
}