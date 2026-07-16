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
            output.print("> "); output.flush()
            val line = input.readLine() ?: return
            try {
                when (val command = CommandParser.parse(line)) {
                    is CliCommand.Help -> output.println(help(command.question))
                    CliCommand.Status -> output.println(status())
                    CliCommand.Reindex -> output.println(reindex())
                    CliCommand.Exit -> return
                    is CliCommand.Invalid -> if (command.message.isNotEmpty()) output.println(command.message)
                }
            } catch (error: Exception) {
                output.println("Error: ${error.message ?: error::class.simpleName}")
                if (debug) error.printStackTrace(output)
            }
            output.flush()
        }
    }
}
