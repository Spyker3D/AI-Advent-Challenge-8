package com.aiassistant.core.domain.support

data class SupportRequest(val question: String, val ticketId: String?, val conversationHistory: List<SupportMessage> = emptyList())
data class SupportMessage(val role: String, val content: String)
data class DeviceInfo(val platform: String, val osVersion: String, val manufacturer: String, val model: String)
data class AppInfo(val version: String, val buildNumber: Int)
data class AppSettings(val historyEnabled: Boolean, val selectedModel: String, val theme: String)
data class SupportUserContext(val id: String, val displayName: String, val device: DeviceInfo, val app: AppInfo, val settings: AppSettings)
data class TicketDiagnostics(val networkAvailable: Boolean?, val lastErrorCode: String?, val lastErrorAt: String? = null, val failedAttempts: Int = 0, val historyEnabled: Boolean? = null)
data class SupportTicket(val id: String, val supportUserId: String, val status: String, val category: String, val priority: String, val subject: String, val description: String, val diagnostics: TicketDiagnostics)
data class KnowledgeChunk(val text: String, val source: String, val title: String)
data class SupportTicketContext(val ticket: SupportTicket, val supportUser: SupportUserContext)
data class SupportSource(val path: String)
data class SupportResponse(val answer: String, val sources: List<SupportSource>, val escalationRecommended: Boolean, val escalationReason: String?, val ticket: SupportTicket? = null, val supportUser: SupportUserContext? = null)
data class EscalationDecision(val recommended: Boolean, val reason: String? = null)

sealed class SupportFailure(message: String) : RuntimeException(message) {
    class InvalidQuestion : SupportFailure("Введите вопрос о работе AI Assistant.")
    class TicketNotFound : SupportFailure("Демонстрационный тикет не найден.")
    class McpUnavailable : SupportFailure("Не удалось получить технические данные тикета.")
    class RagUnavailable : SupportFailure("База знаний поддержки временно недоступна.")
    class OpenAiUnavailable : SupportFailure("AI-сервис временно недоступен. Повторите запрос.")
}

interface SupportTicketProvider {
    suspend fun getTicket(ticketId: String): Result<SupportTicketContext>
    suspend fun listTickets(): Result<List<SupportTicketContext>>
}
interface SupportKnowledgeProvider { suspend fun search(query: String): Result<List<KnowledgeChunk>> }
interface SupportAssistantService { suspend fun answer(request: SupportRequest): SupportResponse }
