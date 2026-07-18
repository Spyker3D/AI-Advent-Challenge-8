package com.aiassistant.core.data.support

import com.aiassistant.core.data.mcp.McpClient
import com.aiassistant.core.domain.support.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpSupportTicketProvider @Inject constructor(private val client: McpClient, private val gson: Gson) : SupportTicketProvider {
    override suspend fun getTicket(ticketId: String): Result<SupportTicketContext> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = extract(client.callTool(SUPPORT_MCP_ENDPOINT, "get_ticket", mapOf("ticketId" to ticketId)))
            if (!payload.get("found").asBoolean) {
                if (payload.getAsJsonObject("error")?.get("code")?.asString == "TICKET_NOT_FOUND") throw SupportFailure.TicketNotFound()
                throw SupportFailure.McpUnavailable()
            }
            SupportTicketContext(gson.fromJson(payload["ticket"], SupportTicket::class.java), gson.fromJson(payload["supportUser"], SupportUserContext::class.java))
        }
    }
    override suspend fun listTickets(): Result<List<SupportTicketContext>> = withContext(Dispatchers.IO) {
        runCatching {
            val list = extract(client.callTool(SUPPORT_MCP_ENDPOINT, "list_tickets", emptyMap())).getAsJsonArray("tickets")
            list.map { element ->
                val ticket = gson.fromJson(element, SupportTicket::class.java)
                getTicket(ticket.id).getOrThrow()
            }
        }
    }
    private fun extract(raw: String): JsonObject {
        if (raw.startsWith("HTTP") || raw.startsWith("MCP request error")) throw SupportFailure.McpUnavailable()
        val root = gson.fromJson(raw, JsonObject::class.java)
        val text = root.getAsJsonObject("result").getAsJsonArray("content")[0].asJsonObject["text"].asString
        return gson.fromJson(text, JsonObject::class.java)
    }
    private companion object { const val SUPPORT_MCP_ENDPOINT = "http://10.0.2.2:3000/mcp" }
}
