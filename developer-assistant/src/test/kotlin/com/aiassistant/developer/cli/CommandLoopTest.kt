package com.aiassistant.developer.cli

import kotlin.test.Test
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
    }
    @Test fun `rejects empty help`() {
        assertEquals(CliCommand.Invalid("Usage: /help <question>"), CommandParser.parse("/help  "))
    }
}
