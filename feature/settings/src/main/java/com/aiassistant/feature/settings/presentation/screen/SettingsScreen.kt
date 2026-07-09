package com.aiassistant.feature.settings.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aiassistant.core.domain.entity.AiModel
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.feature.settings.presentation.SettingsUiEvent
import com.aiassistant.feature.settings.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isProviderDropdownExpanded by remember { mutableStateOf(false) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.handleEvent(SettingsUiEvent.ResetToDefaults) }
                    ) {
                        Text("Reset")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(title = "AI Provider") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = isProviderDropdownExpanded,
                        onExpandedChange = { isProviderDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.settings.provider.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = isProviderDropdownExpanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isProviderDropdownExpanded,
                            onDismissRequest = { isProviderDropdownExpanded = false }
                        ) {
                            AiProvider.values().forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.displayName()) },
                                    onClick = {
                                        viewModel.handleEvent(SettingsUiEvent.ProviderChanged(provider))
                                        isProviderDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (uiState.settings.provider == AiProvider.LOCAL_OLLAMA) {
                        OutlinedTextField(
                            value = uiState.settings.localBaseUrl,
                            onValueChange = {
                                viewModel.handleEvent(SettingsUiEvent.LocalBaseUrlChanged(it))
                            },
                            label = { Text("Ollama Base URL") },
                            placeholder = { Text(ChatSettings.DEFAULT_LOCAL_BASE_URL) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.settings.localModel,
                            onValueChange = {
                                viewModel.handleEvent(SettingsUiEvent.LocalModelChanged(it))
                            },
                            label = { Text("Ollama Model") },
                            placeholder = { Text(ChatSettings.DEFAULT_LOCAL_MODEL) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Android Emulator: http://10.0.2.2:11434\nReal phone on same Wi-Fi: http://COMPUTER_IP:11434",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Model Selection
            SettingsCard(title = "AI Model") {
                ExposedDropdownMenuBox(
                    expanded = isModelDropdownExpanded,
                    onExpandedChange = { isModelDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.settings.selectedModel.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = isModelDropdownExpanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isModelDropdownExpanded,
                        onDismissRequest = { isModelDropdownExpanded = false }
                    ) {
                        AiModel.values().forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.displayName) },
                                onClick = {
                                    viewModel.handleEvent(SettingsUiEvent.ModelChanged(model))
                                    isModelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Temperature Slider
            SettingsCard(title = "Temperature") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Creativity",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = String.format("%.1f", uiState.settings.temperature),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Slider(
                        value = uiState.settings.temperature,
                        onValueChange = { 
                            viewModel.handleEvent(SettingsUiEvent.TemperatureChanged(it))
                        },
                        valueRange = ChatSettings.MIN_TEMPERATURE..ChatSettings.MAX_TEMPERATURE,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Focused",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Creative",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Max Tokens
            SettingsCard(title = "Max Tokens") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Response length limit",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = uiState.settings.maxTokens.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Slider(
                        value = uiState.settings.maxTokens.toFloat(),
                        onValueChange = { 
                            viewModel.handleEvent(SettingsUiEvent.MaxTokensChanged(it.toInt()))
                        },
                        valueRange = ChatSettings.MIN_MAX_TOKENS.toFloat()..ChatSettings.MAX_MAX_TOKENS.toFloat(),
                        steps = 99, // (10000-10)/100 = 99.9, rounded down
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // System Prompt
            SettingsCard(title = "System Prompt") {
                OutlinedTextField(
                    value = uiState.settings.systemPrompt,
                    onValueChange = { 
                        viewModel.handleEvent(SettingsUiEvent.SystemPromptChanged(it))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Define the AI assistant's behavior and personality...") },
                    maxLines = 5
                )
            }

            // Day 2 - Response Format Control
            SettingsCard(title = "Response Format Control") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Use JSON format")
                        Switch(
                            checked = uiState.settings.useJsonFormat,
                            onCheckedChange = { viewModel.handleEvent(SettingsUiEvent.UseJsonFormatChanged(it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Limit response length")
                        Switch(
                            checked = uiState.settings.limitLength,
                            onCheckedChange = { viewModel.handleEvent(SettingsUiEvent.LimitLengthChanged(it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Use Stop Sequence")
                        Switch(
                            checked = uiState.settings.useStopSequence,
                            onCheckedChange = { viewModel.handleEvent(SettingsUiEvent.UseStopSequenceChanged(it)) }
                        )
                    }

                    // Stop Sequence Text Field (visible only when Use Stop Sequence is enabled)
                    if (uiState.settings.useStopSequence) {
                        OutlinedTextField(
                            value = uiState.settings.stopSequenceText,
                            onValueChange = { viewModel.handleEvent(SettingsUiEvent.StopSequenceChanged(it)) },
                            label = { Text("Stop Sequence") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Context Compression Settings
            SettingsCard(title = "Context Compression") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Use Context Compression Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Use Context Compression")
                        Switch(
                            checked = uiState.settings.useContextCompression,
                            onCheckedChange = { viewModel.handleEvent(SettingsUiEvent.UseContextCompressionChanged(it)) }
                        )
                    }

                    // Keep Last Messages Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Keep Last Messages")
                        
                        var keepLastExpanded by remember { mutableStateOf(false) }
                        val keepLastOptions = listOf(4, 6, 8, 10)

                        ExposedDropdownMenuBox(
                            expanded = keepLastExpanded,
                            onExpandedChange = { keepLastExpanded = it }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.menuAnchor(),
                                value = uiState.settings.keepLastMessagesCount.toString(),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keepLastExpanded) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                label = { Text("Messages") }
                            )
                            ExposedDropdownMenu(
                                expanded = keepLastExpanded,
                                onDismissRequest = { keepLastExpanded = false }
                            ) {
                                keepLastOptions.forEach { count ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                count.toString(),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        onClick = {
                                            viewModel.handleEvent(
                                                SettingsUiEvent.KeepLastMessagesCountChanged(
                                                    count
                                                )
                                            )
                                            keepLastExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Clear Summary Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clear Summary")
                        // Note: This would require implementing a way to communicate with the chat ViewModel
                        // For now, we'll leave this as a placeholder
                        Button(
                            onClick = { /* TODO: Implement clear summary functionality */ },
                            enabled = false // Disabled until implemented
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }
}

private fun AiProvider.displayName(): String {
    return when (this) {
        AiProvider.OPENROUTER -> "Online OpenRouter"
        AiProvider.LOCAL_OLLAMA -> "Local Ollama"
    }
}

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            content()
        }
    }
}
