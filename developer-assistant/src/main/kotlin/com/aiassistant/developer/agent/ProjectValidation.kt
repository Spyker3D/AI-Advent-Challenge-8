package com.aiassistant.developer.agent

import com.google.gson.JsonParser

data class ValidationCheck(val passed: Boolean, val name: String, val evidence: String, val recommendation: String? = null)

object ProjectValidation {
    fun validateTickets(ticketsJson: String, usersJson: String): List<ValidationCheck> {
        val tickets = JsonParser.parseString(ticketsJson).asJsonArray.map { it.asJsonObject }
        val users = JsonParser.parseString(usersJson).asJsonArray.map { it.asJsonObject }
        val required = setOf("id", "supportUserId", "status", "category", "subject", "diagnostics")
        val missing = tickets.flatMapIndexed { index, ticket -> required.filterNot(ticket::has).map { "ticket[$index].$it" } }
        val ticketIds = tickets.mapNotNull { it.get("id")?.asString }
        val userIds = users.mapNotNull { it.get("id")?.asString }
        val dangling = tickets.mapNotNull { it.get("supportUserId")?.asString }.filterNot { it in userIds }.distinct()
        return listOf(
            ValidationCheck(missing.isEmpty(), "Required ticket fields", if (missing.isEmpty()) "All ${tickets.size} tickets contain required fields." else "Missing: ${missing.joinToString()}", "Add every required field."),
            ValidationCheck(ticketIds.size == ticketIds.distinct().size, "Unique ticket IDs", "Checked ${ticketIds.size} ticket IDs.", "Replace duplicate ticket IDs."),
            ValidationCheck(userIds.size == userIds.distinct().size, "Unique user IDs", "Checked ${userIds.size} user IDs.", "Replace duplicate user IDs."),
            ValidationCheck(dangling.isEmpty(), "Valid supportUserId references", if (dangling.isEmpty()) "Every ticket user exists." else "Unknown users: ${dangling.joinToString()}", "Add users or fix ticket references.")
        )
    }
}
