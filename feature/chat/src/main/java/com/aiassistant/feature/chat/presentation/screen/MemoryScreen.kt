package com.aiassistant.feature.chat.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aiassistant.feature.chat.presentation.memory.MemoryFileType
import com.aiassistant.feature.chat.presentation.viewmodel.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit,
    onEditTaskContext: () -> Unit,
    onEditMarkdownMemory: (MemoryFileType) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMemory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            WorkingMemoryCard(
                taskContext = uiState.activeTaskContext,
                onPause = viewModel::pauseTask,
                onResume = viewModel::resumeTask,
                onContinue = viewModel::continueTask,
                onEdit = onEditTaskContext
            )

            Spacer(modifier = Modifier.height(16.dp))

            LongTermMemoryCard(onEditMarkdownMemory = onEditMarkdownMemory)
        }
    }
}

@Composable
private fun WorkingMemoryCard(
    taskContext: com.aiassistant.core.domain.memory.TaskContext?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onContinue: () -> Unit,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Working Memory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Task-specific context.\nCan be reused across multiple chats.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Active task:\n${taskContext?.id ?: "None"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            taskContext?.let {
                Text(
                    text = "Stage: ${it.taskState.stage.name}\n" +
                        "Status: ${it.taskState.status.name}\n" +
                        "Current step: ${it.taskState.currentStep}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPause,
                        enabled = it.taskState.status !=
                            com.aiassistant.core.domain.memory.TaskRunStatus.PAUSED &&
                            it.taskState.status !=
                            com.aiassistant.core.domain.memory.TaskRunStatus.COMPLETED
                    ) {
                        Text("Pause")
                    }
                    Button(
                        onClick = onResume,
                        enabled = it.taskState.status ==
                            com.aiassistant.core.domain.memory.TaskRunStatus.PAUSED
                    ) {
                        Text("Resume")
                    }
                    Button(
                        onClick = onContinue,
                        enabled = it.taskState.status ==
                            com.aiassistant.core.domain.memory.TaskRunStatus.WAITING_USER
                    ) {
                        Text("Continue")
                    }
                }
            }
            Button(
                onClick = onEdit,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Edit Task Context")
            }
        }
    }
}

@Composable
private fun LongTermMemoryCard(
    onEditMarkdownMemory: (MemoryFileType) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Long-term Memory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            MemoryFileType.values().forEach { type ->
                ListItem(
                    headlineContent = { Text(type.title) },
                    supportingContent = { Text("Markdown memory file") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingContent = {
                        Button(onClick = { onEditMarkdownMemory(type) }) {
                            Text("Edit")
                        }
                    }
                )
            }
        }
    }
}
