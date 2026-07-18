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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aiassistant.core.domain.entity.AiProvider
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.feature.settings.presentation.SettingsUiEvent
import com.aiassistant.feature.settings.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSupport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isProviderDropdownExpanded by remember { mutableStateOf(false) }
    var isVpsApiKeyVisible by remember { mutableStateOf(false) }
    var infoText by remember { mutableStateOf<String?>(null) }

    infoText?.let { text ->
        AlertDialog(
            onDismissRequest = { infoText = null },
            confirmButton = { TextButton(onClick = { infoText = null }) { Text("OK") } },
            text = { Text(text) }
        )
    }

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
            SettingsCard(title = "Поддержка") {
                Text("Помощь по работе с AI Assistant и демонстрационным техническим тикетам.")
                Button(onClick = onNavigateToSupport) { Text("Открыть поддержку") }
            }
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

                    if (uiState.settings.provider == AiProvider.OPENAI) {
                        OutlinedTextField(
                            value = uiState.settings.openAiModel,
                            onValueChange = {
                                viewModel.handleEvent(SettingsUiEvent.OpenAiModelChanged(it))
                            },
                            label = { Text("Online model") },
                            placeholder = { Text(ChatSettings.DEFAULT_OPENAI_MODEL) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (uiState.settings.provider == AiProvider.LOCAL_OLLAMA) {
                        OutlinedTextField(
                            value = uiState.settings.localBaseUrl,
                            onValueChange = {
                                viewModel.handleEvent(SettingsUiEvent.LocalBaseUrlChanged(it))
                            },
                            label = { Text("Ollama Base URL") },
                            placeholder = { Text(ChatSettings.DEFAULT_LOCAL_BASE_URL) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        StringSettingDropdown(
                            label = "Model",
                            value = uiState.settings.localModel,
                            values = listOf(
                                "qwen2.5:7b-instruct",
                                "qwen2.5:7b-instruct-q4_K_M",
                                "qwen2.5:7b-instruct-q5_K_M",
                                "qwen2.5:7b-instruct-q8_0"
                            ),
                            onSelected = { viewModel.handleEvent(SettingsUiEvent.LocalModelChanged(it)) }
                        )
                        Text(
                            "Q4_K_M: меньше памяти, обычно быстрее, немного ниже точность.\n" +
                                "Q5_K_M: больше памяти, немного медленнее, потенциально выше качество.\n" +
                                "Результат зависит от запроса; можно вручную ввести любой Ollama tag.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LocalSliderSetting("Temperature", uiState.settings.localTemperature, 0f..1.5f, 30,
                            { infoText = "Чем ниже значение, тем более точные и предсказуемые ответы. Чем выше — тем более разнообразные." },
                            { viewModel.handleEvent(SettingsUiEvent.LocalTemperatureChanged((it * 20).toInt() / 20f)) })

                        IntSettingDropdown("Max output tokens", uiState.settings.localMaxTokens,
                            listOf(128, 256, 512, 700, 1024, 2048), true,
                            { infoText = "Максимальная длина ответа." },
                            { viewModel.handleEvent(SettingsUiEvent.LocalMaxTokensChanged(it)) })

                        IntSettingDropdown("Context window", uiState.settings.localContextWindow,
                            listOf(2048, 4096, 8192, 16384), false,
                            { infoText = "Максимальный объём контекста, который модель может учитывать. Больший контекст увеличивает расход памяти." },
                            { viewModel.handleEvent(SettingsUiEvent.LocalContextWindowChanged(it)) })

                        LocalSliderSetting("Top P", uiState.settings.localTopP, 0.1f..1f, 17,
                            { infoText = "Ограничивает разнообразие выбираемых токенов." },
                            { viewModel.handleEvent(SettingsUiEvent.LocalTopPChanged(it)) })

                        LocalSliderSetting("Repeat penalty", uiState.settings.localRepeatPenalty, 0.8f..1.5f, 13,
                            { infoText = "Уменьшает повторения одинаковых слов и предложений." },
                            { viewModel.handleEvent(SettingsUiEvent.LocalRepeatPenaltyChanged(it)) })

                        SettingLabel("Seed") { infoText = "Позволяет получать более повторяемые результаты." }
                        OutlinedTextField(
                            value = uiState.settings.localSeed?.toString().orEmpty(),
                            onValueChange = { viewModel.handleEvent(SettingsUiEvent.LocalSeedChanged(it.toIntOrNull())) },
                            label = { Text("Seed (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        SettingLabel("System prompt") { infoText = "Инструкция, определяющая поведение локальной модели." }
                        OutlinedTextField(
                            value = uiState.settings.localSystemPrompt,
                            onValueChange = { viewModel.handleEvent(SettingsUiEvent.LocalSystemPromptChanged(it)) },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Android Emulator: http://10.0.2.2:11434\nReal phone on same Wi-Fi: http://COMPUTER_IP:11434",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        OutlinedTextField(
                            value = uiState.settings.privateVpsBaseUrl,
                            onValueChange = { viewModel.handleEvent(SettingsUiEvent.PrivateVpsBaseUrlChanged(it)) },
                            label = { Text("VPS Base URL") },
                            placeholder = { Text("http://123.123.123.123/") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.settings.privateVpsModel,
                            onValueChange = { viewModel.handleEvent(SettingsUiEvent.PrivateVpsModelChanged(it)) },
                            label = { Text("VPS Model") },
                            placeholder = { Text(ChatSettings.DEFAULT_PRIVATE_VPS_MODEL) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.settings.privateVpsApiKey,
                            onValueChange = { viewModel.handleEvent(SettingsUiEvent.PrivateVpsApiKeyChanged(it)) },
                            label = { Text("VPS API Key") },
                            singleLine = true,
                            visualTransformation = if (isVpsApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { isVpsApiKeyVisible = !isVpsApiKeyVisible }) {
                                    Text(if (isVpsApiKeyVisible) "Hide" else "Show")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { viewModel.handleEvent(SettingsUiEvent.TestPrivateVpsConnection) },
                            enabled = !uiState.isLoading
                        ) {
                            Text(if (uiState.isLoading) "Testing..." else "Test VPS connection")
                        }
                        uiState.vpsTestResult?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Text(
                            text = "HTTP sends the Bearer token without transport encryption. Use HTTPS in production.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SettingsCard(title = "Invariants") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Invariants")
                        Text(
                            "Если выключить, ответы не будут проверяться и блокироваться правилами Invariants.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.settings.invariantsEnabled,
                        onCheckedChange = {
                            viewModel.handleEvent(SettingsUiEvent.InvariantsEnabledChanged(it))
                        }
                    )
                }
            }

            SettingsCard(title = "Task pipeline") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Task pipeline")
                        Text(
                            "Если выключить, запросы будут сразу отправляться в обычный чат без этапов PLANNING, EXECUTION и VALIDATION.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.settings.taskPipelineEnabled,
                        onCheckedChange = {
                            viewModel.handleEvent(SettingsUiEvent.TaskPipelineEnabledChanged(it))
                        }
                    )
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

@Composable
private fun SettingLabel(label: String, onInfo: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Medium)
        TextButton(onClick = onInfo) { Text("Info") }
    }
}

@Composable
private fun LocalSliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onInfo: () -> Unit,
    onChange: (Float) -> Unit
) {
    Column {
        SettingLabel(label, onInfo)
        Text(
            text = "Текущее значение: ${String.format("%.2f", value)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StringSettingDropdown(label: String, value: String, values: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = value, onValueChange = onSelected, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { option -> DropdownMenuItem(text = { Text(localModelDisplayName(option)) }, onClick = {
                onSelected(option); expanded = false
            }) }
        }
    }
}

private fun localModelDisplayName(tag: String): String = when (tag) {
    "qwen2.5:7b-instruct" -> "Qwen 2.5 7B — Q4_K_M"
    "qwen2.5:7b-instruct-q5_K_M" -> "Qwen 2.5 7B — Q5_K_M"
    "qwen2.5:7b-instruct-q4_K_M" -> "Qwen 2.5 7B — Q4_K_M (explicit tag)"
    "qwen2.5:7b-instruct-q8_0" -> "Qwen 2.5 7B — Q8_0"
    else -> tag
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntSettingDropdown(
    label: String,
    value: Int,
    values: List<Int>,
    allowCustom: Boolean,
    onInfo: () -> Unit,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var custom by remember(value) { mutableStateOf(allowCustom && value !in values) }
    SettingLabel(label, onInfo)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = if (custom) "Custom" else value.toString(), onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { option -> DropdownMenuItem(text = { Text(option.toString()) }, onClick = {
                custom = false; onSelected(option); expanded = false
            }) }
            if (allowCustom) DropdownMenuItem(text = { Text("Custom") }, onClick = { custom = true; expanded = false })
        }
    }
    if (custom) OutlinedTextField(value = value.toString(), onValueChange = { it.toIntOrNull()?.let(onSelected) },
        label = { Text("Custom value (128–4096)") }, modifier = Modifier.fillMaxWidth())
}

private fun AiProvider.displayName(): String {
    return when (this) {
        AiProvider.OPENAI -> "OpenAI"
        AiProvider.LOCAL_OLLAMA -> "Local Ollama"
        AiProvider.PRIVATE_VPS -> "Private VPS"
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
