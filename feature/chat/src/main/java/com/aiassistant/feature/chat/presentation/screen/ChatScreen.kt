package com.aiassistant.feature.chat.presentation.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.aiassistant.core.domain.entity.AiResponseMetadata
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.entity.TokenMetrics
import com.aiassistant.core.ui.components.LoadingIndicator
import com.aiassistant.core.ui.components.MessageBubble
import com.aiassistant.feature.chat.presentation.ChatUiEvent
import com.aiassistant.feature.chat.presentation.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Sealed class for different item types in the chat
sealed class ChatItem {
    data class MessageItem(val message: com.aiassistant.core.domain.entity.Message) : ChatItem()
    data class TokenMetricsItem(val tokenMetrics: com.aiassistant.core.domain.entity.TokenMetrics) : ChatItem()
    object LoadingIndicatorItem : ChatItem()
}

@Suppress("EXPERIMENTAL_API_USAGE")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    // val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Refresh settings when returning from settings screen
    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { selectedUri ->
                processSelectedFile(selectedUri, context, viewModel)
            }
        }
    )

    // Auto-scroll to bottom when new messages are added or when loading starts
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        // Re-calculate chat items to determine the actual item count
        val chatItems = mutableListOf<ChatItem>()
        uiState.messages.forEach { message ->
            chatItems.add(ChatItem.MessageItem(message))
            // Add token metrics as separate items for assistant messages
            message.tokenMetrics?.let { tokenMetrics ->
                if (message.role == com.aiassistant.core.domain.entity.MessageRole.ASSISTANT) {
                    chatItems.add(ChatItem.TokenMetricsItem(tokenMetrics))
                }
            }
            // Add loading indicator only after the last user message when loading
            if (message.role == com.aiassistant.core.domain.entity.MessageRole.USER && 
                uiState.isLoading && 
                message == uiState.messages.lastOrNull { it.role == com.aiassistant.core.domain.entity.MessageRole.USER }) {
                chatItems.add(ChatItem.LoadingIndicatorItem)
            }
        }
        
        // Scroll to the last item
        if (chatItems.isNotEmpty()) {
            listState.animateScrollToItem(chatItems.size - 1)
        }
    }

    // Show error as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.handleEvent(ChatUiEvent.ClearError)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Assistant",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.handleEvent(ChatUiEvent.ClearChat) }) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Clear Chat"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
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
                // .imePadding()
                // .navigationBarsPadding()
        ) {
            // Messages list (existing chat functionality)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.messages.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start a conversation with AI",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Create a list that includes messages, token metrics, and loading indicators
                        val chatItems = mutableListOf<ChatItem>()
                        uiState.messages.forEach { message ->
                            chatItems.add(ChatItem.MessageItem(message))
                            // Add token metrics as separate items for assistant messages
                            message.tokenMetrics?.let { tokenMetrics ->
                                if (message.role == com.aiassistant.core.domain.entity.MessageRole.ASSISTANT) {
                                    chatItems.add(ChatItem.TokenMetricsItem(tokenMetrics))
                                }
                            }
                            // Add loading indicator only after the last user message when loading
                            if (message.role == com.aiassistant.core.domain.entity.MessageRole.USER && 
                                uiState.isLoading && 
                                message == uiState.messages.lastOrNull { it.role == com.aiassistant.core.domain.entity.MessageRole.USER }) {
                                chatItems.add(ChatItem.LoadingIndicatorItem)
                            }
                        }
                        
                        items(chatItems) { item ->
                            when (item) {
                                is ChatItem.MessageItem -> {
                                    val message = item.message
                                    // Show structured cards if JSON parsing succeeded for assistant messages
                                    if (message.role == com.aiassistant.core.domain.entity.MessageRole.ASSISTANT && uiState.useJsonFormat) {
                                        // Try to parse the message content as FormattedAiResponse
                                        val formattedResponse = viewModel.parseFormattedResponse(message.content)
                                        if (formattedResponse != null) {
                                            // Don't show the message bubble for successful JSON parsing
                                            // Show structured cards only
                                            
                                            // Topic
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = "Topic",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(text = formattedResponse.topic, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                            
                                            // Date
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = "Date",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(text = formattedResponse.date, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                            
                                            // Time
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = "Time",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(text = formattedResponse.time, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                            
                                            // Answer
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = "Answer",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(text = formattedResponse.answer, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                            
                                            // Tags
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = "Tags",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.Start
                                                    ) {
                                                        formattedResponse.tags.forEach { tag ->
                                                            Card(
                                                                modifier = Modifier.padding(end = 4.dp),
                                                                shape = RoundedCornerShape(16.dp),
                                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = tag,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            // Show compression info for JSON formatted responses
                                            val tokenMetrics = message.tokenMetrics
                                            if (tokenMetrics != null) {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 4.dp),
                                                    elevation = CardDefaults.cardElevation(
                                                        defaultElevation = 4.dp
                                                    )
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text(
                                                            text = buildCompressionInfoText(
                                                                tokenMetrics,
                                                                uiState.useContextCompression
                                                            ),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            // Show message bubble if JSON parsing failed but JSON format was requested
                                            EnhancedMessageBubble(
                                                message = message,
                                                useContextCompression = uiState.useContextCompression,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            
                                            // Show raw response card
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = "Invalid JSON format, showing raw response",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Show message bubble for all other messages (user messages or non-JSON assistant messages)
                                        if (message.role == com.aiassistant.core.domain.entity.MessageRole.ASSISTANT) {
                                            // Show enhanced message bubble with compression info for assistant messages
                                            EnhancedMessageBubble(
                                                message = message,
                                                useContextCompression = uiState.useContextCompression,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            MessageBubble(
                                                message = message,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                is ChatItem.TokenMetricsItem -> {
                                    // Show token metrics as a separate message
                                    TokenMetricsMessage(
                                        tokenMetrics = item.tokenMetrics,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                is ChatItem.LoadingIndicatorItem -> {
                                    LoadingIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // Loading indicator is now shown after user messages
                        // This ensures it appears in the right place in the conversation flow
                    }
                }
            }

            // File attachment section
            if (uiState.attachedFileName != null) {
                AttachedFileSection(
                    fileName = uiState.attachedFileName!!,
                    fileText = uiState.attachedFileText ?: "",
                    onClear = { viewModel.handleEvent(ChatUiEvent.ClearAttachedFile) }
                )
            }
            
            // Context Compression Section
            if (uiState.useContextCompression) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Context Compression Metrics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Full History Tokens: ${uiState.fullHistoryTokensEstimate}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Compressed Tokens: ${uiState.compressedHistoryTokensEstimate}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Saved Tokens: ${uiState.savedTokensEstimate}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Compression Ratio: ${uiState.compressionRatioPercent}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            
            // Input section (existing chat input)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attach file button
                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch("text/plain")
                    },
                    enabled = !uiState.isLoading
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Attach file")
                }
                
                OutlinedTextField(
                    value = uiState.currentMessage,
                    onValueChange = { viewModel.handleEvent(ChatUiEvent.MessageChanged(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type your message...", style = MaterialTheme.typography.bodyMedium) },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            viewModel.handleEvent(ChatUiEvent.SendMessage)
                            // keyboardController?.hide()
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                FloatingActionButton(
                    onClick = {
                        viewModel.handleEvent(ChatUiEvent.SendMessage)
                        // keyboardController?.hide()
                    },
                    modifier = Modifier.padding(bottom = 4.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}

private fun buildTokenText(metadata: AiResponseMetadata): String {
    val promptTokens = metadata.promptTokens ?: "unavailable"
    val completionTokens = metadata.completionTokens ?: "unavailable"
    val totalTokens = metadata.totalTokens ?: "unavailable"
    return "Tokens: $promptTokens input / $completionTokens output / $totalTokens total"
}

private fun buildCostText(metadata: AiResponseMetadata): String {
    val cost = metadata.estimatedCostUsd?.let { String.format("$%.6f", it) } ?: "unavailable"
    return "Cost: $cost"
}

@Composable
fun TokenMetricsMessage(
    tokenMetrics: TokenMetrics,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(0.85f)
        ) {
            Text(
                text = buildTokenMetricsText(tokenMetrics),
                color = contentColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun buildTokenMetricsText(tokenMetrics: TokenMetrics): String {
    val completionTokens = tokenMetrics.completionTokens?.toString() ?: "unavailable"
    return "Request tokens: ${tokenMetrics.currentRequestTokens} | " +
           "History tokens: ${tokenMetrics.historyTokens} | " +
           "Completion tokens: $completionTokens"
}

@Composable
fun AttachedFileSection(
    fileName: String,
    fileText: String,
    onClear: () -> Unit
) {
    val tokenCount = estimateTokenCount(fileText)
    val isOverLimit = tokenCount > 8000
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attached file: $fileName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Clear attachment")
                }
            }
            
            Text(
                text = "Estimated file tokens: $tokenCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isOverLimit) {
                Text(
                    text = "⚠ Context may exceed model limits",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Simple token estimation (roughly 4 characters per token)
// This is a rough approximation for display purposes only
fun estimateTokenCount(text: String): Int {
    return text.length / 4
}

// Process selected file
fun processSelectedFile(uri: Uri, context: android.content.Context, viewModel: ChatViewModel) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = getFileName(context, uri)
        
        inputStream?.use { stream ->
            val content = stream.bufferedReader().use { it.readText() }
            viewModel.handleEvent(ChatUiEvent.FileAttached(fileName, content))
        }
    } catch (e: Exception) {
        // Handle error silently or show a message
    }
}

// Get file name from URI
fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = it.getString(nameIndex)
                }
            }
        }
    }
    
    if (result == null) {
        result = uri.path?.substringAfterLast("/")
    }
    
    return result ?: "Unknown file"
}

@Composable
fun EnhancedMessageBubble(
    message: Message,
    useContextCompression: Boolean,
    modifier: Modifier = Modifier
) {
    val isUserMessage = message.role == MessageRole.USER
    val backgroundColor = if (isUserMessage) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUserMessage) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUserMessage) 16.dp else 4.dp,
                        bottomEnd = if (isUserMessage) 4.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .padding(12.dp)
                .let {
                    if (isUserMessage) it else it.fillMaxWidth(0.85f)
                }
        ) {
            val context = LocalContext.current
            Column {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.content,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Show compression info for assistant messages
                    val tokenMetrics = message.tokenMetrics
                    if (!isUserMessage && tokenMetrics != null) {
                        Text(
                            text = buildCompressionInfoText(
                                tokenMetrics,
                                useContextCompression
                            ),
                            color = contentColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }

                    // Copy button for assistant messages
                    if (!isUserMessage) {
                        IconButton(
                            onClick = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Message", message.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT)
                                    .show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Text(
                    text = formatTimestamp(message.timestamp),
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

private fun buildCompressionInfoText(
    tokenMetrics: TokenMetrics,
    useContextCompression: Boolean
): String {
    val completionTokens = tokenMetrics.completionTokens?.toString() ?: "unavailable"
    val compressionStatus = if (useContextCompression) "ON" else "OFF"
    return "Compression: $compressionStatus | History Tokens: ${tokenMetrics.historyTokens} | Completion Tokens: $completionTokens"
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}