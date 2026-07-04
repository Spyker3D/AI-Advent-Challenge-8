package com.aiassistant.feature.chat.presentation.memory

enum class MemoryFileType(val routeValue: String, val title: String) {
    PROFILE("profile", "Profile"),
    PREFERENCES("preferences", "Preferences"),
    GLOBAL_RULES("global_rules", "Global Rules"),
    PROJECT_KNOWLEDGE("project_knowledge", "Project Knowledge"),
    DECISIONS("decisions", "Decisions");

    companion object {
        fun fromRouteValue(value: String): MemoryFileType {
            return values().firstOrNull { it.routeValue == value } ?: GLOBAL_RULES
        }
    }
}
