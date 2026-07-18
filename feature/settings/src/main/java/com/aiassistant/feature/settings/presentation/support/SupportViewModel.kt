package com.aiassistant.feature.settings.presentation.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.core.domain.support.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SupportUiState { data object Idle: SupportUiState; data object Loading: SupportUiState; data class Success(val response: SupportResponse): SupportUiState; data class Error(val message: String, val canRetry: Boolean): SupportUiState }
data class SupportScreenState(val tickets: List<SupportTicketContext> = emptyList(), val selected: SupportTicketContext? = null, val question: String = "", val messages: List<SupportMessage> = emptyList(), val result: SupportUiState = SupportUiState.Idle)

class SupportViewModel @Inject constructor(private val service: SupportAssistantService, private val ticketProvider: SupportTicketProvider): ViewModel() {
    private val mutable = MutableStateFlow(SupportScreenState())
    val state: StateFlow<SupportScreenState> = mutable.asStateFlow()
    init { loadTickets() }
    fun loadTickets() = viewModelScope.launch { ticketProvider.listTickets().onSuccess { mutable.value = mutable.value.copy(tickets = it, selected = mutable.value.selected ?: it.firstOrNull()) }.onFailure { mutable.value = mutable.value.copy(result = SupportUiState.Error("Не удалось загрузить демонстрационные тикеты.", true)) } }
    fun select(id: String) { mutable.value.tickets.firstOrNull { it.ticket.id == id }?.let { mutable.value = mutable.value.copy(selected = it, result = SupportUiState.Idle) } }
    fun question(value: String) { mutable.value = mutable.value.copy(question = value) }
    fun send() { val current = mutable.value; if (current.question.isBlank()) return; viewModelScope.launch {
        mutable.value = current.copy(result = SupportUiState.Loading)
        runCatching { service.answer(SupportRequest(current.question, current.selected?.ticket?.id, current.messages)) }
            .onSuccess { response -> mutable.value = mutable.value.copy(question = "", messages = current.messages + SupportMessage("user", current.question) + SupportMessage("assistant", response.answer), result = SupportUiState.Success(response)) }
            .onFailure { mutable.value = mutable.value.copy(result = SupportUiState.Error(it.message ?: "Неизвестная ошибка поддержки.", true)) }
    } }
}
