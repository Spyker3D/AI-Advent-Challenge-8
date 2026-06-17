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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiassistant.core.domain.memory.TaskContext
import com.aiassistant.feature.chat.presentation.viewmodel.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskContextEditorScreen(
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val taskContext = uiState.activeTaskContext

    var taskId by remember { mutableStateOf(taskContext?.id ?: "memory_layers_assignment") }
    var title by remember { mutableStateOf(taskContext?.title ?: "Memory Layers Assignment") }
    var description by remember { mutableStateOf(taskContext?.description ?: "") }
    var goals by remember { mutableStateOf(taskContext?.goals?.joinToString("\n") ?: "") }
    var constraints by remember { mutableStateOf(taskContext?.constraints?.joinToString("\n") ?: "") }
    var decisions by remember { mutableStateOf(taskContext?.decisions?.joinToString("\n") ?: "") }
    var currentState by remember { mutableStateOf(taskContext?.currentState ?: "") }
    var relatedChats by remember { mutableStateOf(taskContext?.relatedChatIds?.joinToString("\n") ?: "") }

    LaunchedEffect(taskContext?.id) {
        taskContext?.let {
            taskId = it.id
            title = it.title
            description = it.description
            goals = it.goals.joinToString("\n")
            constraints = it.constraints.joinToString("\n")
            decisions = it.decisions.joinToString("\n")
            currentState = it.currentState
            relatedChats = it.relatedChatIds.joinToString("\n")
        }
    }

    LaunchedEffect(uiState.message, uiState.error) {
        val message = uiState.message ?: uiState.error
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Context") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveTaskContext(
                                TaskContext(
                                    id = taskId.trim(),
                                    title = title.trim(),
                                    description = description.trim(),
                                    relatedChatIds = relatedChats.toListByLine(),
                                    goals = goals.toListByLine(),
                                    constraints = constraints.toListByLine(),
                                    decisions = decisions.toListByLine(),
                                    currentState = currentState.trim()
                                )
                            )
                        },
                        enabled = taskId.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            MemoryTextField("Task ID", taskId, { taskId = it }, singleLine = true)
            MemoryTextField("Title", title, { title = it }, singleLine = true)
            MemoryTextField("Description", description, { description = it })
            MemoryTextField("Goals", goals, { goals = it })
            MemoryTextField("Constraints", constraints, { constraints = it })
            MemoryTextField("Decisions", decisions, { decisions = it })
            MemoryTextField("Current State", currentState, { currentState = it })
            MemoryTextField("Related Chats", relatedChats, { relatedChats = it })
        }
    }
}

@Composable
private fun MemoryTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
        minLines = if (singleLine) 1 else 4
    )
    Spacer(modifier = Modifier.height(12.dp))
}

private fun String.toListByLine(): List<String> {
    return lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
