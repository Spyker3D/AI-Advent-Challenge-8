package com.aiassistant.developer.agent

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectValidationTest {
    @Test fun `detects duplicate ticket IDs and dangling support users`() {
        val tickets = """[
          {"id":"t1","supportUserId":"u1","status":"open","category":"x","subject":"a","diagnostics":{}},
          {"id":"t1","supportUserId":"missing","status":"open","category":"x","subject":"b","diagnostics":{}}
        ]"""
        val users = """[{"id":"u1"},{"id":"u1"}]"""
        val checks = ProjectValidation.validateTickets(tickets, users)
        assertFalse(checks.single { it.name == "Unique ticket IDs" }.passed)
        assertFalse(checks.single { it.name == "Unique user IDs" }.passed)
        assertFalse(checks.single { it.name == "Valid supportUserId references" }.passed)
        assertTrue(checks.single { it.name == "Required ticket fields" }.passed)
    }
}
