package com.aiassistant.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aiassistant.di.ViewModelFactory
import com.aiassistant.feature.chat.presentation.memory.MemoryFileType
import com.aiassistant.feature.chat.presentation.screen.ChatScreen
import com.aiassistant.feature.chat.presentation.screen.Day2Screen
import com.aiassistant.feature.chat.presentation.screen.MarkdownMemoryEditorScreen
import com.aiassistant.feature.chat.presentation.screen.InvariantsEditorScreen
import com.aiassistant.feature.chat.presentation.screen.MemoryScreen
import com.aiassistant.feature.chat.presentation.screen.TaskContextEditorScreen
import com.aiassistant.feature.chat.presentation.viewmodel.ChatViewModel
import com.aiassistant.feature.chat.presentation.viewmodel.MemoryViewModel
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
            // Refresh settings when returning to chat screen
            chatViewModel.refreshSettings()
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToMemory = {
                    navController.navigate(Screen.Memory.route)
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

        composable(Screen.Memory.route) {
            val memoryViewModel: MemoryViewModel = viewModel(factory = viewModelFactory)
            MemoryScreen(
                viewModel = memoryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditTaskContext = {
                    navController.navigate(Screen.TaskContextEditor.route)
                },
                onEditMarkdownMemory = { type ->
                    navController.navigate(Screen.MarkdownMemoryEditor.createRoute(type.routeValue))
                },
                onEditInvariants = {
                    navController.navigate(Screen.InvariantsEditor.route)
                }
            )
        }

        composable(Screen.InvariantsEditor.route) {
            val memoryViewModel: MemoryViewModel = viewModel(factory = viewModelFactory)
            InvariantsEditorScreen(
                viewModel = memoryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TaskContextEditor.route) {
            val memoryViewModel: MemoryViewModel = viewModel(factory = viewModelFactory)
            TaskContextEditorScreen(
                viewModel = memoryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MarkdownMemoryEditor.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val memoryViewModel: MemoryViewModel = viewModel(factory = viewModelFactory)
            val type = MemoryFileType.fromRouteValue(
                backStackEntry.arguments?.getString("type").orEmpty()
            )
            MarkdownMemoryEditorScreen(
                viewModel = memoryViewModel,
                type = type,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Day2.route) {
            val chatViewModel: ChatViewModel = viewModel(factory = viewModelFactory)
            // Refresh settings when returning to day2 screen
            chatViewModel.refreshSettings()
            Day2Screen(
                viewModel = chatViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
    }
}
