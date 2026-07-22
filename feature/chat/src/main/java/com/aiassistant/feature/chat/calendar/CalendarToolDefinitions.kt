package com.aiassistant.feature.chat.calendar

import com.google.gson.Gson
import com.google.gson.JsonObject

object CalendarToolDefinitions {
    val supported = setOf("list_calendar_events", "find_calendar_events", "create_calendar_event", "update_calendar_event", "delete_calendar_event", "clarification")
    fun parseCall(raw: String, gson: Gson = Gson()): Result<Pair<String, String>> = runCatching {
        val clean = raw.replace("```json", "").replace("```", "").trim()
        val objectValue = gson.fromJson(clean, JsonObject::class.java) ?: error("Empty JSON")
        val name = objectValue.get("tool")?.asString ?: error("Missing tool")
        require(name in supported) { "Unknown calendar tool: $name" }
        val arguments = objectValue.getAsJsonObject("arguments") ?: error("Missing arguments")
        name to gson.toJson(arguments)
    }
}
