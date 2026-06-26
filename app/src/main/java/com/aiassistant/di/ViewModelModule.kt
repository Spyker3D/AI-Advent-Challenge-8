package com.aiassistant.di

import androidx.lifecycle.ViewModel
import com.aiassistant.feature.chat.presentation.mcp.McpDemoViewModel
import com.aiassistant.feature.chat.presentation.viewmodel.ChatViewModel
import com.aiassistant.feature.chat.presentation.viewmodel.MemoryViewModel
import com.aiassistant.feature.settings.presentation.viewmodel.SettingsViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel::class)
    abstract fun bindChatViewModel(chatViewModel: ChatViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MemoryViewModel::class)
    abstract fun bindMemoryViewModel(memoryViewModel: MemoryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(settingsViewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(McpDemoViewModel::class)
    abstract fun bindMcpDemoViewModel(viewModel: McpDemoViewModel): ViewModel
}
