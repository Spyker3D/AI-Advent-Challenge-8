package com.aiassistant.navigation

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Settings : Screen("settings")
    object Day2 : Screen("day2")
    object Memory : Screen("memory")
    object TaskContextEditor : Screen("memory/task-context")
    object MarkdownMemoryEditor : Screen("memory/markdown/{type}") {
        fun createRoute(type: String): String = "memory/markdown/$type"
    }
}
