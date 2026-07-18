package com.aiassistant.developer.cli

sealed interface CliCommand {
    data class Help(val question: String) : CliCommand
    data object Status : CliCommand
    data object Reindex : CliCommand
    data object Diff : CliCommand
    data class Goal(val text: String) : CliCommand
    data object Exit : CliCommand
    data class Invalid(val message: String) : CliCommand
}

object CommandParser {
    fun parse(input: String): CliCommand {
        val value = input.trim()
        return when {
            value == "/exit" -> CliCommand.Exit
            value == "/status" -> CliCommand.Status
            value == "/reindex" -> CliCommand.Reindex
            value == "/diff" -> CliCommand.Diff
            value == "/help" -> CliCommand.Invalid("Usage: /help <question>")
            value.startsWith("/help ") -> value.removePrefix("/help ").trim().let {
                if (it.isBlank()) CliCommand.Invalid("Usage: /help <question>") else CliCommand.Help(it)
            }
            value.isBlank() -> CliCommand.Invalid("")
            value.startsWith("/") -> CliCommand.Invalid("Unknown command. Use /help <question>, /status, /reindex, /diff, or /exit.")
            else -> CliCommand.Goal(value)
        }
    }
}
