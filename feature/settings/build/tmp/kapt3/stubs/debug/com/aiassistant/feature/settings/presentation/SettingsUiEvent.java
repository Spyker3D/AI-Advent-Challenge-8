package com.aiassistant.feature.settings.presentation;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0006\u0003\u0004\u0005\u0006\u0007\bB\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0006\t\n\u000b\f\r\u000e\u00a8\u0006\u000f"}, d2 = {"Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent;", "", "()V", "MaxTokensChanged", "ModelChanged", "ResetToDefaults", "SaveSettings", "SystemPromptChanged", "TemperatureChanged", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$MaxTokensChanged;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$ModelChanged;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$ResetToDefaults;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$SaveSettings;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$SystemPromptChanged;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$TemperatureChanged;", "settings_debug"})
public abstract class SettingsUiEvent {
    
    private SettingsUiEvent() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$MaxTokensChanged;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent;", "maxTokens", "", "(I)V", "getMaxTokens", "()I", "component1", "copy", "equals", "", "other", "", "hashCode", "toString", "", "settings_debug"})
    public static final class MaxTokensChanged extends com.aiassistant.feature.settings.presentation.SettingsUiEvent {
        private final int maxTokens = 0;
        
        public MaxTokensChanged(int maxTokens) {
        }
        
        public final int getMaxTokens() {
            return 0;
        }
        
        public final int component1() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.aiassistant.feature.settings.presentation.SettingsUiEvent.MaxTokensChanged copy(int maxTokens) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$ModelChanged;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent;", "model", "Lcom/aiassistant/core/domain/entity/AiModel;", "(Lcom/aiassistant/core/domain/entity/AiModel;)V", "getModel", "()Lcom/aiassistant/core/domain/entity/AiModel;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "settings_debug"})
    public static final class ModelChanged extends com.aiassistant.feature.settings.presentation.SettingsUiEvent {
        @org.jetbrains.annotations.NotNull()
        private final com.aiassistant.core.domain.entity.AiModel model = null;
        
        public ModelChanged(@org.jetbrains.annotations.NotNull()
        com.aiassistant.core.domain.entity.AiModel model) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.aiassistant.core.domain.entity.AiModel getModel() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.aiassistant.core.domain.entity.AiModel component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.aiassistant.feature.settings.presentation.SettingsUiEvent.ModelChanged copy(@org.jetbrains.annotations.NotNull()
        com.aiassistant.core.domain.entity.AiModel model) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$ResetToDefaults;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent;", "()V", "settings_debug"})
    public static final class ResetToDefaults extends com.aiassistant.feature.settings.presentation.SettingsUiEvent {
        @org.jetbrains.annotations.NotNull()
        public static final com.aiassistant.feature.settings.presentation.SettingsUiEvent.ResetToDefaults INSTANCE = null;
        
        private ResetToDefaults() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$SaveSettings;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent;", "()V", "settings_debug"})
    public static final class SaveSettings extends com.aiassistant.feature.settings.presentation.SettingsUiEvent {
        @org.jetbrains.annotations.NotNull()
        public static final com.aiassistant.feature.settings.presentation.SettingsUiEvent.SaveSettings INSTANCE = null;
        
        private SaveSettings() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$SystemPromptChanged;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent;", "systemPrompt", "", "(Ljava/lang/String;)V", "getSystemPrompt", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "settings_debug"})
    public static final class SystemPromptChanged extends com.aiassistant.feature.settings.presentation.SettingsUiEvent {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String systemPrompt = null;
        
        public SystemPromptChanged(@org.jetbrains.annotations.NotNull()
        java.lang.String systemPrompt) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getSystemPrompt() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.aiassistant.feature.settings.presentation.SettingsUiEvent.SystemPromptChanged copy(@org.jetbrains.annotations.NotNull()
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
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent$TemperatureChanged;", "Lcom/aiassistant/feature/settings/presentation/SettingsUiEvent;", "temperature", "", "(F)V", "getTemperature", "()F", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "settings_debug"})
    public static final class TemperatureChanged extends com.aiassistant.feature.settings.presentation.SettingsUiEvent {
        private final float temperature = 0.0F;
        
        public TemperatureChanged(float temperature) {
        }
        
        public final float getTemperature() {
            return 0.0F;
        }
        
        public final float component1() {
            return 0.0F;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.aiassistant.feature.settings.presentation.SettingsUiEvent.TemperatureChanged copy(float temperature) {
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
}