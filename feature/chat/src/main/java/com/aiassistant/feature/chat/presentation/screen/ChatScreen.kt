package com.aiassistant.feature.chat.presentation.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiassistant.core.domain.entity.AiResponseMetadata
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.entity.TokenMetrics
import com.aiassistant.core.domain.mcp.McpExecutionLogItem
import com.aiassistant.core.domain.mcp.McpExecutionStatus
import com.aiassistant.core.ui.components.LoadingIndicator
import com.aiassistant.core.ui.components.MessageBubble
import com.aiassistant.feature.chat.presentation.ChatUiEvent
import com.aiassistant.feature.chat.presentation.RagSourceUi
import com.aiassistant.feature.chat.presentation.viewmodel.ChatViewModel
import com.aiassistant.feature.chat.calendar.CalendarDateTime
import com.aiassistant.feature.chat.calendar.CalendarUiState
import kotlinx.coroutines.launch
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
    onNavigateToMemory: () -> Unit,
    onNavigateToMcpDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    // val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var isOverflowMenuOpen by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context as? android.app.Activity
        val state = uiState.calendarState as? CalendarUiState.PermissionRequired
        val permanentlyDenied = !granted && permissionRequested && state != null && activity?.shouldShowRequestPermissionRationale(state.permission) == false
        viewModel.onCalendarPermissionResult(granted, permanentlyDenied)
    }

    val permissionState = uiState.calendarState as? CalendarUiState.PermissionRequired
    if (permissionState != null) {
        AlertDialog(onDismissRequest = viewModel::dismissCalendarPermission, title = { Text("Доступ к календарю") }, text = { Text(if (permissionState.permission == android.Manifest.permission.READ_CALENDAR) "Для просмотра реальных событий приложению нужен доступ к календарю." else "Для добавления подтверждённого события приложению нужен доступ на запись в календарь.") }, confirmButton = { TextButton(onClick = { if (permissionState.permanentlyDenied) { context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) } else { permissionRequested = true; calendarPermissionLauncher.launch(permissionState.permission) } }) { Text(if (permissionState.permanentlyDenied) "Открыть настройки" else "Продолжить") } }, dismissButton = { TextButton(onClick = viewModel::dismissCalendarPermission) { Text("Не сейчас") } })
    }

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
                message == uiState.messages.lastOrNull { it.role == com.aiassistant.core.domain.entity.MessageRole.USER }
            ) {
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("AI Assistant", modifier = Modifier.padding(16.dp))

                Button(
                    onClick = {
                        viewModel.handleEvent(ChatUiEvent.NewChatClicked)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("+ New chat")
                }

                Text("Chats:", modifier = Modifier.padding(16.dp, 0.dp))

                uiState.chats.forEach { chat ->
                    NavigationDrawerItem(
                        label = { Text(chat.title) },
                        selected = chat.id == uiState.currentChatId,
                        onClick = {
                            viewModel.handleEvent(ChatUiEvent.ChatSelected(chat.id))
                            scope.launch { drawerState.close() }
                        },
                        badge = {
                            IconButton(
                                onClick = {
                                    viewModel.handleEvent(ChatUiEvent.DeleteChatClicked(chat.id))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete chat"
                                )
                            }
                        },
                        modifier = Modifier.padding(8.dp, 0.dp)
                    )
                }
            }
        }
    ) {
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
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open chats")
                        }
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
                        IconButton(onClick = { isOverflowMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More"
                            )
                        }
                        DropdownMenu(
                            expanded = isOverflowMenuOpen,
                            onDismissRequest = { isOverflowMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (uiState.ragEnabled) "RAG ON" else "RAG OFF") },
                                onClick = {
                                    viewModel.handleEvent(ChatUiEvent.RagToggled(!uiState.ragEnabled))
                                },
                                trailingIcon = {
                                    Switch(
                                        checked = uiState.ragEnabled,
                                        onCheckedChange = {
                                            viewModel.handleEvent(ChatUiEvent.RagToggled(it))
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.day23ImprovedRetrievalEnabled) {
                                            "Day23 Improved"
                                        } else {
                                            "Day23 Baseline"
                                        }
                                    )
                                },
                                onClick = {
                                    if (uiState.ragEnabled) {
                                        viewModel.handleEvent(
                                            ChatUiEvent.Day23ImprovedRetrievalToggled(
                                                !uiState.day23ImprovedRetrievalEnabled
                                            )
                                        )
                                    }
                                },
                                enabled = uiState.ragEnabled,
                                trailingIcon = {
                                    Switch(
                                        checked = uiState.day23ImprovedRetrievalEnabled,
                                        enabled = uiState.ragEnabled,
                                        onCheckedChange = {
                                            viewModel.handleEvent(
                                                ChatUiEvent.Day23ImprovedRetrievalToggled(it)
                                            )
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Memory") },
                                onClick = {
                                    isOverflowMenuOpen = false
                                    onNavigateToMemory()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("MCP Day 17 Demo") },
                                onClick = {
                                    isOverflowMenuOpen = false
                                    onNavigateToMcpDemo()
                                }
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
                uiState.activeTaskContext?.let { taskContext ->
                    TaskStateCard(
                        taskContext = taskContext,
                        onPause = { viewModel.handleEvent(ChatUiEvent.PauseTask) },
                        onResume = { viewModel.handleEvent(ChatUiEvent.ResumeTask) },
                        onContinue = { viewModel.handleEvent(ChatUiEvent.ContinueTask) }
                    )
                }

                // Context Strategy Selector
                ContextStrategySelector(
                    selectedStrategy = uiState.selectedContextStrategy,
                    onStrategySelected = { strategy ->
                        viewModel.handleEvent(ChatUiEvent.ContextStrategySelected(strategy))
                    }
                )

                // Branching Strategy UI
                if (uiState.selectedContextStrategy == com.aiassistant.core.domain.entity.ContextStrategy.BRANCHING) {
                    BranchingControls(
                        branches = uiState.branches,
                        currentBranchId = uiState.currentBranchId,
                        onCreateBranch = { branchName ->
                            viewModel.handleEvent(ChatUiEvent.CreateBranch(branchName))
                        },
                        onSwitchBranch = { branchId ->
                            viewModel.handleEvent(ChatUiEvent.SwitchBranch(branchId))
                        },
                        onDeleteBranch = { branchId ->
                            viewModel.handleEvent(ChatUiEvent.DeleteBranch(branchId))
                        }
                    )
                }

                // Sticky Facts Strategy UI
                if (uiState.selectedContextStrategy == com.aiassistant.core.domain.entity.ContextStrategy.STICKY_FACTS) {
                    StickyFactsDisplay(
                        stickyFacts = uiState.stickyFacts,
                        factsStatus = uiState.factsStatus
                    )
                }

                // Sliding Window Strategy UI
                if (uiState.selectedContextStrategy == com.aiassistant.core.domain.entity.ContextStrategy.SLIDING_WINDOW) {
                    SlidingWindowMetrics(
                        totalMessages = uiState.messages.size
                    )
                }

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
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
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
                                    message == uiState.messages.lastOrNull { it.role == com.aiassistant.core.domain.entity.MessageRole.USER }
                                ) {
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
                                            val formattedResponse =
                                                viewModel.parseFormattedResponse(message.content)
                                            if (formattedResponse != null) {
                                                // Don't show the message bubble for successful JSON parsing
                                                // Show structured cards only

                                                // Topic
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
                                                            text = "Topic",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = formattedResponse.topic,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }

                                                // Date
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
                                                            text = "Date",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = formattedResponse.date,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }

                                                // Time
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
                                                            text = "Time",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = formattedResponse.time,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }

                                                // Answer
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
                                                            text = "Answer",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = formattedResponse.answer,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }

                                                // Tags
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
                                                                    elevation = CardDefaults.cardElevation(
                                                                        defaultElevation = 2.dp
                                                                    )
                                                                ) {
                                                                    Text(
                                                                        text = tag,
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        modifier = Modifier.padding(
                                                                            horizontal = 6.dp,
                                                                            vertical = 2.dp
                                                                        )
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
                                                    elevation = CardDefaults.cardElevation(
                                                        defaultElevation = 4.dp
                                                    )
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
                                        if (message.role == com.aiassistant.core.domain.entity.MessageRole.ASSISTANT) {
                                            uiState.ragSourcesByMessageId[message.id]?.takeIf { it.isNotEmpty() }?.let { sources ->
                                                RagSourcesBlock(
                                                    sources = sources,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    is ChatItem.TokenMetricsItem -> {
                                        // Show token metrics as a separate message
                                        TokenMetricsMessage(
                                            tokenMetrics = item.tokenMetrics,
                                            contextStrategy = uiState.selectedContextStrategy,
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

                (uiState.calendarState as? CalendarUiState.PendingConfirmation)?.let { pending ->
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(CalendarDateTime.formatPreview(pending.action), style = MaterialTheme.typography.bodyMedium)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = viewModel::cancelCalendarAction) { Text("Отмена") }
                                Button(onClick = viewModel::confirmCalendarAction) { Text("Подтвердить") }
                            }
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

                McpExecutionCard(
                    logs = uiState.mcpExecutionLogs,
                    isVisible = uiState.isMcpExecutionVisible
                )

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
                        placeholder = {
                            Text(
                                "Type your message...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
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
}

@Composable
private fun McpExecutionCard(
    logs: List<McpExecutionLogItem>,
    isVisible: Boolean
) {
    var isExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(isVisible, logs.size) {
        if (isVisible && logs.size <= 1) {
            isExpanded = true
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MCP Execution",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.KeyboardArrowDown
                        } else {
                            Icons.Default.KeyboardArrowUp
                        },
                        contentDescription = if (isExpanded) {
                            "Collapse MCP execution"
                        } else {
                            "Expand MCP execution"
                        }
                    )
                }
            }

            if (!isVisible || logs.isEmpty()) {
                Text(
                    text = "Ожидание запроса...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!isExpanded) {
                logs.lastOrNull()?.let { item ->
                    Text(
                        text = "${item.statusPrefix()} ${item.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = item.statusColor()
                    )
                }
            } else {
                logs.forEach { item ->
                    Text(
                        text = "${item.statusPrefix()} ${item.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = item.statusColor()
                    )
                }
            }
        }
    }
}

private fun McpExecutionLogItem.statusPrefix(): String {
    return when (status) {
        McpExecutionStatus.INFO -> "ℹ"
        McpExecutionStatus.RUNNING -> "⏳"
        McpExecutionStatus.SUCCESS -> "✅"
        McpExecutionStatus.ERROR -> "❌"
    }
}

@Composable
private fun McpExecutionLogItem.statusColor(): Color {
    return when (status) {
        McpExecutionStatus.ERROR -> MaterialTheme.colorScheme.error
        McpExecutionStatus.SUCCESS -> MaterialTheme.colorScheme.primary
        McpExecutionStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
        McpExecutionStatus.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun TaskStateCard(
    taskContext: com.aiassistant.core.domain.memory.TaskContext,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onContinue: () -> Unit
) {
    val status = taskContext.taskState.status
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Task State", fontWeight = FontWeight.Bold)
            Text("Stage: ${taskContext.taskState.stage.name}")
            Text("Status: ${status.name}")
            Text("Current step: ${taskContext.taskState.currentStep}")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPause,
                    enabled = status != com.aiassistant.core.domain.memory.TaskRunStatus.PAUSED &&
                        status != com.aiassistant.core.domain.memory.TaskRunStatus.COMPLETED
                ) {
                    Text("Pause")
                }
                OutlinedButton(
                    onClick = onResume,
                    enabled = status == com.aiassistant.core.domain.memory.TaskRunStatus.PAUSED
                ) {
                    Text("Resume")
                }
                Button(
                    onClick = onContinue,
                    enabled = status == com.aiassistant.core.domain.memory.TaskRunStatus.WAITING_USER
                ) {
                    Text("Continue")
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
    contextStrategy: com.aiassistant.core.domain.entity.ContextStrategy?,
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
            Column {
                // Context Strategy
                contextStrategy?.let { strategy ->
                    Text(
                                                text = "Context Strategy: ${when (strategy) {
                            com.aiassistant.core.domain.entity.ContextStrategy.NO_STRATEGY -> "Full History"
                            com.aiassistant.core.domain.entity.ContextStrategy.SLIDING_WINDOW -> "Sliding Window"
                            com.aiassistant.core.domain.entity.ContextStrategy.STICKY_FACTS -> "Sticky Facts"
                            com.aiassistant.core.domain.entity.ContextStrategy.BRANCHING -> "Branching"
                        }}",
                        color = contentColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Token metrics
                Text(
                    text = buildTokenMetricsText(tokenMetrics),
                    color = contentColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
fun ContextStrategySelector(
    selectedStrategy: com.aiassistant.core.domain.entity.ContextStrategy,
    onStrategySelected: (com.aiassistant.core.domain.entity.ContextStrategy) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Context Strategy",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                com.aiassistant.core.domain.entity.ContextStrategy.values().forEach { strategy ->
                    androidx.compose.material3.AssistChip(
                        onClick = { onStrategySelected(strategy) },
                        label = {
                            Text(
                                text = when (strategy) {
                                    com.aiassistant.core.domain.entity.ContextStrategy.NO_STRATEGY -> "Full History"
                                    com.aiassistant.core.domain.entity.ContextStrategy.SLIDING_WINDOW -> "Sliding Window"
                                    com.aiassistant.core.domain.entity.ContextStrategy.STICKY_FACTS -> "Sticky Facts"
                                    com.aiassistant.core.domain.entity.ContextStrategy.BRANCHING -> "Branching"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedStrategy != strategy
                    )
                }
            }
        }
    }
}

@Composable
fun SlidingWindowMetrics(
    totalMessages: Int
) {
    val SLIDING_WINDOW_SIZE = 5
    val messagesSent = minOf(totalMessages, SLIDING_WINDOW_SIZE)
    val messagesOmitted = if (totalMessages > SLIDING_WINDOW_SIZE) totalMessages - SLIDING_WINDOW_SIZE else 0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Sliding Window Strategy",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Messages sent: $messagesSent",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Messages omitted: $messagesOmitted",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun StickyFactsDisplay(
    stickyFacts: com.aiassistant.core.domain.entity.StickyFacts,
    factsStatus: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sticky Facts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Status: $factsStatus",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (factsStatus) {
                        "Updating" -> MaterialTheme.colorScheme.primary
                        "Updated" -> MaterialTheme.colorScheme.tertiary
                        "Failed" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            Text(
                text = "Goal: ${stickyFacts.goal}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Stack: ${stickyFacts.stack}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Constraints: ${stickyFacts.constraints}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Preferences: ${stickyFacts.preferences}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Decisions: ${stickyFacts.decisions}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Unresolved Questions: ${stickyFacts.unresolvedQuestions}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun BranchingControls(
    branches: List<com.aiassistant.core.domain.entity.ChatBranch>,
    currentBranchId: String,
    onCreateBranch: (String) -> Unit,
    onSwitchBranch: (String) -> Unit,
    onDeleteBranch: (String) -> Unit
) {
    var showCreateBranchDialog by remember { mutableStateOf(false) }
    var newBranchName by remember { mutableStateOf("") }
    
    val currentBranch = branches.find { it.id == currentBranchId }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Branching Strategy",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current: ${currentBranch?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Row {
                    OutlinedButton(
                        onClick = { showCreateBranchDialog = true },
                        modifier = Modifier.padding(start = 4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Create", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    if (currentBranchId != "main") {
                        OutlinedButton(
                            onClick = { onDeleteBranch(currentBranchId) },
                            modifier = Modifier.padding(start = 4.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            enabled = currentBranchId != "main"
                        ) {
                            Text("Delete", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            if (branches.size > 1) {
                Text(
                    text = "Branches:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                branches.forEach { branch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = branch.name,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Row {
                            if (branch.id != currentBranchId) {
                                OutlinedButton(
                                    onClick = { onSwitchBranch(branch.id) },
                                    modifier = Modifier.padding(start = 4.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Switch", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text(
                                    text = "(current)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            if (branch.id != "main") {
                                OutlinedButton(
                                    onClick = { onDeleteBranch(branch.id) },
                                    modifier = Modifier.padding(start = 4.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    enabled = branch.id != "main"
                                ) {
                                    Text("Del", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showCreateBranchDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateBranchDialog = false },
            title = { Text("Create New Branch") },
            text = {
                OutlinedTextField(
                    value = newBranchName,
                    onValueChange = { newBranchName = it },
                    label = { Text("Branch Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBranchName.isNotBlank()) {
                            onCreateBranch(newBranchName)
                            newBranchName = ""
                            showCreateBranchDialog = false
                        }
                    },
                    enabled = newBranchName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showCreateBranchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



@Composable
fun RagSourcesBlock(
    sources: List<RagSourceUi>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Sources:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            sources.forEach { source ->
                val scoreText = if (source.improvedRetrieval) {
                    "final=${String.format(Locale.US, "%.2f", source.finalScore)} " +
                        "cosine=${String.format(Locale.US, "%.2f", source.cosineScore)} " +
                        "keyword=${String.format(Locale.US, "%.2f", source.keywordScore)} " +
                        "metadata=${String.format(Locale.US, "%.2f", source.metadataScore)}"
                } else {
                    "cosine=${String.format(Locale.US, "%.2f", source.cosineScore)}"
                }
                Text(
                    text = "- ${source.source} / ${source.section ?: "N/A"} / $scoreText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedMessageBubble(
    message: Message,
    useContextCompression: Boolean,
    modifier: Modifier = Modifier
) {
    var showLocalMetrics by remember { mutableStateOf(false) }
    val localMetrics = message.metadata?.localMetrics
    if (showLocalMetrics && localMetrics != null) {
        ModalBottomSheet(onDismissRequest = { showLocalMetrics = false }) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Local generation details", style = MaterialTheme.typography.titleLarge)
                listOf(
                    "Model" to localMetrics.model,
                    "Temperature" to localMetrics.temperature,
                    "Max output tokens" to localMetrics.maxOutputTokens,
                    "Context window" to localMetrics.contextWindow,
                    "Top P" to localMetrics.topP,
                    "Repeat penalty" to localMetrics.repeatPenalty,
                    "Seed" to (localMetrics.seed ?: "random"),
                    "Prompt tokens" to (localMetrics.promptTokens ?: "unavailable"),
                    "Output tokens" to (localMetrics.outputTokens ?: "unavailable"),
                    "Generation time" to localMetrics.generationSeconds?.let { String.format("%.2f sec", it) }.orEmpty(),
                    "Load time" to localMetrics.loadSeconds?.let { String.format("%.2f sec", it) }.orEmpty(),
                    "Tokens/sec" to localMetrics.tokensPerSecond?.let { String.format("%.1f", it) }.orEmpty()
                ).forEach { (name, value) -> Text("$name: $value") }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
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
                    AssistantMessageText(
                        text = message.content,
                        isUserMessage = isUserMessage,
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

                    if (!isUserMessage && localMetrics != null) {
                        Text(
                            text = listOfNotNull(
                                localMetrics.model,
                                localMetrics.generationSeconds?.let { String.format("%.1f sec", it) },
                                localMetrics.tokensPerSecond?.let { String.format("%.1f tok/sec", it) },
                                localMetrics.outputTokens?.let { "$it tokens" }
                            ).joinToString("  •  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.75f),
                            modifier = Modifier.padding(top = 6.dp).clickable { showLocalMetrics = true }
                        )
                    } else if (!isUserMessage && message.metadata?.modelDisplayName?.startsWith("VPS ·") == true) {
                        val metadata = requireNotNull(message.metadata)
                        Text(
                            text = listOfNotNull(
                                metadata.modelDisplayName,
                                String.format("%.1f sec", metadata.responseTimeMs / 1000.0),
                                metadata.tokensPerSecond?.let { String.format("%.1f tok/sec", it) },
                                metadata.completionTokens?.let { "$it tokens" }
                            ).joinToString("  •  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.75f),
                            modifier = Modifier.padding(top = 6.dp)
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

@Composable
private fun AssistantMessageText(
    text: String,
    isUserMessage: Boolean,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    if (isUserMessage) {
        Text(
            text = text,
            color = color,
            style = style,
            modifier = modifier
        )
        return
    }

    MarkdownMessageText(
        text = text,
        color = color,
        style = style,
        modifier = modifier
    )
}

@Composable
private fun MarkdownMessageText(
    text: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        text.lines().forEach { line ->
            val block = parseMarkdownBlock(line, color, style)
            if (block == null) {
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                ClickableText(
                    text = block.text,
                    style = block.style,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = block.topPadding),
                    onClick = { offset ->
                        block.text
                            .getStringAnnotations(URL_TAG, offset, offset)
                            .firstOrNull()
                            ?.let { annotation ->
                                openUrl(context, annotation.item)
                            }
                    }
                )
            }
        }
    }
}

private data class MarkdownBlock(
    val text: AnnotatedString,
    val style: TextStyle,
    val topPadding: androidx.compose.ui.unit.Dp
)

@Composable
private fun parseMarkdownBlock(
    line: String,
    color: Color,
    baseStyle: TextStyle
): MarkdownBlock? {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return null

    val heading = Regex("^(#{1,6})\\s+(.+)$").find(trimmed)
    if (heading != null) {
        val level = heading.groupValues[1].length
        val content = heading.groupValues[2]
        val headingStyle = when (level) {
            1 -> MaterialTheme.typography.titleLarge
            2 -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.titleSmall
        }.copy(color = color, fontWeight = FontWeight.SemiBold)

        return MarkdownBlock(
            text = buildInlineMarkdown(content),
            style = headingStyle,
            topPadding = if (level == 1) 8.dp else 6.dp
        )
    }

    val bullet = Regex("^[-*]\\s+(.+)$").find(trimmed)
    if (bullet != null) {
        return MarkdownBlock(
            text = buildInlineMarkdown("• ${bullet.groupValues[1]}"),
            style = baseStyle.copy(color = color),
            topPadding = 2.dp
        )
    }

    return MarkdownBlock(
        text = buildInlineMarkdown(trimmed),
        style = baseStyle.copy(color = color),
        topPadding = 0.dp
    )
}

private fun buildInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val next = findNextInlineToken(text, index)
            if (next == null) {
                append(text.substring(index))
                break
            }

            if (next.range.first > index) {
                append(text.substring(index, next.range.first))
            }

            when (next) {
                is InlineToken.Link -> {
                    pushStringAnnotation(tag = URL_TAG, annotation = next.url)
                    pushStyle(
                        SpanStyle(
                            color = Color(0xFF1565C0),
                            textDecoration = TextDecoration.Underline
                        )
                    )
                    append(next.label)
                    pop()
                    pop()
                }

                is InlineToken.Bold -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(next.content)
                    pop()
                }

                is InlineToken.Code -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x1A000000)
                        )
                    )
                    append(next.content)
                    pop()
                }
            }

            index = next.range.last + 1
        }
    }
}

private sealed class InlineToken {
    abstract val range: IntRange

    data class Link(
        override val range: IntRange,
        val label: String,
        val url: String
    ) : InlineToken()

    data class Bold(
        override val range: IntRange,
        val content: String
    ) : InlineToken()

    data class Code(
        override val range: IntRange,
        val content: String
    ) : InlineToken()
}

private fun findNextInlineToken(text: String, startIndex: Int): InlineToken? {
    val link = MARKDOWN_LINK_REGEX.find(text, startIndex)?.let {
        InlineToken.Link(
            range = it.range,
            label = it.groupValues[1],
            url = it.groupValues[2]
        )
    }
    val bold = MARKDOWN_BOLD_REGEX.find(text, startIndex)?.let {
        InlineToken.Bold(
            range = it.range,
            content = it.groupValues[1]
        )
    }
    val code = MARKDOWN_CODE_REGEX.find(text, startIndex)?.let {
        InlineToken.Code(
            range = it.range,
            content = it.groupValues[1]
        )
    }

    return listOfNotNull(link, bold, code).minByOrNull { it.range.first }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (throwable: Throwable) {
        Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
    }
}

private const val URL_TAG = "url"
private val MARKDOWN_LINK_REGEX = Regex("\\[([^\\]]+)]\\((https?://[^\\s)]+)\\)")
private val MARKDOWN_BOLD_REGEX = Regex("\\*\\*([^*]+)\\*\\*")
private val MARKDOWN_CODE_REGEX = Regex("`([^`]+)`")

fun buildCompressionInfoText(
    tokenMetrics: TokenMetrics,
    useContextCompression: Boolean
): String {
    val completionTokens = tokenMetrics.completionTokens?.toString() ?: "unavailable"
    val compressionStatus = if (useContextCompression) "ON" else "OFF"
    return "Compression: $compressionStatus | History Tokens: ${tokenMetrics.historyTokens} | Completion Tokens: $completionTokens"
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
