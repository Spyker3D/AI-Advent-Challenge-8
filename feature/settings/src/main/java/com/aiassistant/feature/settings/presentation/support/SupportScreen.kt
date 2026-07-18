package com.aiassistant.feature.settings.presentation.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SupportScreen(viewModel: SupportViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Поддержка AI Assistant") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Демонстрационный тикет", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { state.tickets.forEach { TextButton(onClick = { viewModel.select(it.ticket.id) }) { Text(it.ticket.id) } } }
            state.selected?.let { c -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("Тикет: ${c.ticket.id}", fontWeight = FontWeight.Bold); Text("Проблема: ${c.ticket.subject}"); Text("Устройство: ${c.supportUser.device.model}, Android ${c.supportUser.device.osVersion}"); Text("Версия приложения: ${c.supportUser.app.version}"); Text("Статус: ${if (c.ticket.status == "open") "Открыт" else c.ticket.status}") } } }
            state.messages.forEach { Text("${if (it.role == "user") "Вы" else "Поддержка"}: ${it.content}") }
            OutlinedTextField(state.question, viewModel::question, Modifier.fillMaxWidth(), label = { Text("Ваш вопрос") }, minLines = 2)
            Button(onClick = viewModel::send, enabled = state.result !is SupportUiState.Loading && state.question.isNotBlank()) { Text("Отправить") }
            when (val result = state.result) {
                SupportUiState.Loading -> CircularProgressIndicator()
                is SupportUiState.Error -> { Text(result.message, color = MaterialTheme.colorScheme.error); if (result.canRetry) OutlinedButton(onClick = viewModel::send) { Text("Повторить") } }
                is SupportUiState.Success -> { if (result.response.sources.isNotEmpty()) { Text("Источники", fontWeight = FontWeight.Bold); result.response.sources.forEach { Text("• ${it.path}") } }; if (result.response.escalationRecommended) Card { Column(Modifier.padding(12.dp)) { Text("Рекомендуется оператор", fontWeight = FontWeight.Bold); result.response.escalationReason?.let { Text(it) } } } }
                else -> Unit
            }
        }
    }
}
