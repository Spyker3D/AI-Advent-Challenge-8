package com.aiassistant.developer.cli

import java.io.BufferedReader
import java.io.PrintWriter

class CommandLoop(
    private val input: BufferedReader,
    private val output: PrintWriter,
    private val help: (String) -> String,
    private val status: () -> String,
    private val reindex: () -> String,
    private val debug: Boolean = false
) {
    fun run() {
        while (true) {
            printPrompt()
            val line = input.readLine() ?: return
            try {
                when (val command = CommandParser.parse(line)) {
                    is CliCommand.Help -> printBlock("АССИСТЕНТ", help(command.question))
                    CliCommand.Status -> printBlock("СТАТУС", status())
                    CliCommand.Reindex -> printBlock("ПЕРЕИНДЕКСАЦИЯ", reindex())
                    CliCommand.Exit -> return
                    is CliCommand.Invalid -> if (command.message.isNotEmpty()) printBlock("СИСТЕМА", command.message)
                }
            } catch (error: Exception) {
                printBlock("ОШИБКА", error.message ?: error::class.simpleName.orEmpty())
                if (debug) error.printStackTrace(output)
            }
            output.flush()
        }
    }

    private fun printPrompt() {
        output.println(SEPARATOR)
        output.println("ВЫ:")
        output.print("> ")
        output.flush()
    }

    private fun printBlock(title: String, text: String) {
        output.println()
        output.println("$title:")
        output.println(text)
        output.println()
    }

    private companion object {
        const val SEPARATOR = "────────────────────────────────────────"
    }
}
