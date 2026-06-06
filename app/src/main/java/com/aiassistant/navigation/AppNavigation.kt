package com.aiassistant.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aiassistant.di.ViewModelFactory
import com.aiassistant.feature.chat.presentation.screen.ChatScreen
import com.aiassistant.feature.chat.presentation.viewmodel.ChatViewModel
import com.aiassistant.feature.settings.presentation.screen.SettingsScreen
import com.aiassistant.feature.settings.presentation.viewmodel.SettingsViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModelFactory: ViewModelFactory
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route
    ) {
        composable(Screen.Chat.route) {
            val chatViewModel: ChatViewModel = viewModel(factory = viewModelFactory)
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}