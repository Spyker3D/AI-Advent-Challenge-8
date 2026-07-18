package com.aiassistant.core.domain.support

import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import kotlinx.coroutines.delay
import javax.inject.Inject

class DefaultSupportAssistantService @Inject constructor(
    private val tickets: SupportTicketProvider,
    private val knowledge: SupportKnowledgeProvider,
    private val llm: LlmClient,
    private val promptBuilder: SupportPromptBuilder
) : SupportAssistantService {
    override suspend fun answer(request: SupportRequest): SupportResponse {
        if (request.question.isBlank()) throw SupportFailure.InvalidQuestion()
        var mcpUnavailable = false
        val ticketContext = request.ticketId?.let { id ->
            retryOnce { tickets.getTicket(id) }.getOrElse { error ->
                if (error is SupportFailure.TicketNotFound) throw error
                mcpUnavailable = true; null
            }
        }
        val ragQuery = buildQuery(request.question, ticketContext)
        val chunks = retryOnce { knowledge.search(ragQuery) }.getOrElse { emptyList() }
        val prompt = promptBuilder.build(request, ticketContext, chunks)
        val modelResult = retryOnce { llm.sendChat(listOf(Message("support", prompt, MessageRole.USER)), 700) }
        val answer = modelResult.getOrElse { throw SupportFailure.OpenAiUnavailable() }.message.trim()
        val decision = evaluateEscalation(ticketContext?.ticket, chunks, answer, request.question, mcpUnavailable)
        val fallbackPrefix = if (mcpUnavailable) "Не удалось получить технические данные тикета. Ниже приведены общие рекомендации из документации AI Assistant.\n\n" else ""
        return SupportResponse(fallbackPrefix + answer, chunks.map { SupportSource(it.source) }.distinct(), decision.recommended, decision.reason, ticketContext?.ticket, ticketContext?.supportUser)
    }

    private fun buildQuery(question: String, c: SupportTicketContext?) = buildString {
        appendLine("Проблема: $question")
        c?.let { appendLine("Категория: ${it.ticket.category}.\nОписание: ${it.ticket.subject}.\nКод ошибки: ${it.ticket.diagnostics.lastErrorCode}.\nИнтернет доступен: ${it.ticket.diagnostics.networkAvailable}.\nВерсия приложения: ${it.supportUser.app.version}.\nИстория включена: ${it.supportUser.settings.historyEnabled}.") }
    }.trim()

    private suspend fun <T> retryOnce(block: suspend () -> Result<T>): Result<T> {
        val first = block(); if (first.isSuccess) return first
        delay(100); return block()
    }
}
