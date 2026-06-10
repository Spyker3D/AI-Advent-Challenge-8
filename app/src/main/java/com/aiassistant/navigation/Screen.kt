package com.aiassistant.navigation

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Settings : Screen("settings")
    object Day2 : Screen("day2")
}