package com.aiassistant.feature.chat.presentation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.aiassistant.feature.chat.presentation.memory.MemoryFileType
import com.aiassistant.feature.chat.presentation.viewmodel.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownMemoryEditorScreen(
    viewModel: MemoryViewModel,
    type: MemoryFileType,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var content by remember(type) { mutableStateOf(uiState.contentFor(type)) }

    LaunchedEffect(type, uiState.longTermMemory) {
        content = uiState.contentFor(type)
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
                title = { Text(type.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = { viewModel.saveMarkdownMemory(type, content) }) {
                        Text("Save")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            minLines = 24
        )
    }
}

private fun com.aiassistant.feature.chat.presentation.memory.MemoryUiState.contentFor(
    type: MemoryFileType
): String {
    return when (type) {
        MemoryFileType.PROFILE -> longTermMemory.profile
        MemoryFileType.PREFERENCES -> longTermMemory.preferences
        MemoryFileType.GLOBAL_RULES -> longTermMemory.globalRules
        MemoryFileType.PROJECT_KNOWLEDGE -> longTermMemory.projectKnowledge
        MemoryFileType.DECISIONS -> longTermMemory.decisions
    }
}
