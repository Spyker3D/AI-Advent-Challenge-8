package com.aiassistant.developer.cli

import java.io.BufferedReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CommandLoopTest {
    @Test fun `recognizes help`() {
        assertEquals(CliCommand.Help("Where is config?"), CommandParser.parse("/help Where is config?"))
    }
    @Test fun `recognizes status reindex and exit`() {
        assertIs<CliCommand.Status>(CommandParser.parse("/status"))
        assertIs<CliCommand.Reindex>(CommandParser.parse("/reindex"))
        assertIs<CliCommand.Exit>(CommandParser.parse("/exit"))
        assertIs<CliCommand.Diff>(CommandParser.parse("/diff"))
        assertEquals(CliCommand.Goal("Create a report"), CommandParser.parse("Create a report"))
    }
    @Test fun `rejects empty help`() {
        assertEquals(CliCommand.Invalid("Usage: /help <question>"), CommandParser.parse("/help  "))
    }

    @Test fun `visually separates help conversations`() {
        val text = StringWriter()
        val loop = CommandLoop(
            input = BufferedReader(StringReader("/help First question\n/help Second question\n/exit\n")),
            output = PrintWriter(text, true),
            help = { "Answer to: $it" },
            status = { "status" },
            reindex = { "reindexed" }
        )

        loop.run()

        val output = text.toString()
        assertEquals(3, "────────────────────────────────────────".toRegex().findAll(output).count())
        assertEquals(3, "ВЫ:".toRegex().findAll(output).count())
        assertEquals(2, "АССИСТЕНТ:".toRegex().findAll(output).count())
        assertContains(output, "Answer to: First question")
        assertContains(output, "Answer to: Second question")
    }
}
