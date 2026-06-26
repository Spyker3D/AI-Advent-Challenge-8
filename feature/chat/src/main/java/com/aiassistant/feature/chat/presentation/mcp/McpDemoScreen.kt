package com.aiassistant.feature.chat.presentation.mcp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpDemoScreen(
    viewModel: McpDemoViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MCP Demo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "MCP Day 17 Demo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text("MCP endpoint: http://31.129.110.10:3000/mcp")

            OutlinedTextField(
                value = state.taskId,
                onValueChange = viewModel::onTaskIdChanged,
                label = { Text("Task ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::loadTools,
                    enabled = !state.isLoading
                ) {
                    Text("Показать tools/list")
                }
                Button(
                    onClick = viewModel::callTool,
                    enabled = !state.isLoading
                ) {
                    Text("Вызвать MCP tool")
                }
            }

            McpResultCard(
                title = "Tools list:",
                content = state.toolsList.ifBlank { "Пока не загружено" }
            )
            McpResultCard(
                title = "Task tool result:",
                content = state.result
            )

            Text(
                text = "MCP Day 18: Periodic Weather Summary",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Данные собираются MCP-сервером на VPS по расписанию раз в 60 секунд " +
                    "и сохраняются в JSON."
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::loadWeatherSummary,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Получить weather summary")
                }
                Button(
                    onClick = viewModel::loadWeatherHistory,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Показать weather history")
                }
                Button(
                    onClick = viewModel::collectWeatherNow,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Собрать погоду сейчас")
                }
            }

            Text(
                text = "24/7 Agent Monitoring",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "MCP-сервер на VPS собирает данные по расписанию, а Android-агент " +
                    "каждые 10 секунд запрашивает агрегированную сводку через MCP tool " +
                    "get_weather_summary."
            )
            Button(
                onClick = viewModel::toggleAutoRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isAutoRefreshEnabled) {
                        "Остановить авто-сводку"
                    } else {
                        "Запустить авто-сводку"
                    }
                )
            }
            Text(
                text = if (state.isAutoRefreshEnabled) {
                    "Статус: авто-сводка включена"
                } else {
                    "Статус: авто-сводка выключена"
                }
            )
            Text(
                text = "Последнее обновление: " +
                    state.lastAutoRefreshAt.ifBlank { "ещё не выполнялось" }
            )
            Text("Интервал обновления: ${state.autoRefreshIntervalSec} секунд")

            if (state.isLoading) {
                CircularProgressIndicator()
            }

            McpResultCard(
                title = "Weather tool result:",
                content = state.weatherResult
            )
        }
    }
}

@Composable
private fun McpResultCard(
    title: String,
    content: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                text = content,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
