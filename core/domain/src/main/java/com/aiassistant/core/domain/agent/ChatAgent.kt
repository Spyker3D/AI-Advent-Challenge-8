package com.aiassistant.core.domain.agent

import android.util.Log
import com.aiassistant.core.domain.entity.AiChatResponse
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.ContextStrategy

import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.entity.StickyFacts
import com.aiassistant.core.domain.entity.TokenMetrics
import com.aiassistant.core.domain.memory.MemoryContext
import com.aiassistant.core.domain.memory.MemoryOrchestrator
import com.aiassistant.core.domain.memory.PromptBuilder
import com.aiassistant.core.domain.util.TokenCounter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class ChatAgent @Inject constructor(
    private val llmClient: LlmClient,
    private val memoryOrchestrator: MemoryOrchestrator,
    private val promptBuilder: PromptBuilder,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    // Current branch ID - default to "main"
    private var currentBranchId: String = "main"
    
    // Current sticky facts
    private var currentStickyFacts: StickyFacts = StickyFacts()
    
    // Set the current branch
    fun setCurrentBranch(branchId: String) {
        currentBranchId = branchId
    }
    
    // Set the current sticky facts
    fun setCurrentStickyFacts(stickyFacts: StickyFacts) {
        currentStickyFacts = stickyFacts
    }
    

    
    /**
     * Build system prompt with sticky facts
     */
    private fun buildSystemPromptWithFacts(originalPrompt: String?): String {
        return buildString {
            append(originalPrompt ?: "")
            append("\n\nImportant facts:\n\n")
            append("Goal:\n${currentStickyFacts.goal}\n\n")
            append("Stack:\n${currentStickyFacts.stack}\n\n")
            append("Constraints:\n${currentStickyFacts.constraints}\n\n")
            append("Preferences:\n${currentStickyFacts.preferences}\n\n")
            append("Decisions:\n${currentStickyFacts.decisions}\n\n")
            append("Unresolved Questions:\n${currentStickyFacts.unresolvedQuestions}\n\n")
        }
    }
    
    /**
     * Sends a message to the LLM and manages the chat history
     */
    suspend fun sendMessage(
        chatRequest: ChatRequest,
        contextStrategy: ContextStrategy = ContextStrategy.SLIDING_WINDOW,
        chatId: String? = null,
        taskContextId: String? = null
    ): Result<AiChatResponse> = withContext(dispatcher) {
        try {
            val memoryContext = buildMemoryContext(chatId, taskContextId, contextStrategy)
            val enrichedSystemPrompt = buildEnrichedSystemPrompt(chatRequest, memoryContext)
            logMemoryLayers(chatId, memoryContext)

            // Use the history provided in the chatRequest
            val effectiveHistory = buildEffectiveHistory(
                chatRequest = chatRequest,
                contextStrategy = contextStrategy,
                memoryContext = memoryContext
            )
            
            // Create the current user message from chatRequest.message
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                content = chatRequest.message,
                role = MessageRole.USER
            )
            
            // Add the current user message to the history
            effectiveHistory.add(userMessage)
            
            // Log the history size for debugging
            Log.d("ChatAgent", "Sending history size: ${effectiveHistory.size}")
            
            // Debug logging for BRANCHING strategy
            if (contextStrategy == ContextStrategy.BRANCHING) {
                Log.d("BRANCHING_FINAL_REQUEST", "History messages:")
                effectiveHistory.forEachIndexed { index, message ->
                    Log.d("BRANCHING_FINAL_REQUEST", "index=$index role=${message.role} content=${message.content.take(50)}...")
                }
                Log.d("BRANCHING_FINAL_REQUEST", "currentMessage=${chatRequest.message}")
            }
            
            // Calculate token metrics before sending to LLM
            val currentRequestTokens = TokenCounter.countTokens(chatRequest.message)
            val historyTokens = effectiveHistory.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
                .sumOf { TokenCounter.countTokens(it.content) }
            
            // For sticky facts strategy, do not modify the system prompt as it's already handled by ViewModel
            // The system prompt with facts is already in the first message of effectiveHistory
            val effectiveSystemPrompt = enrichedSystemPrompt
            
            // Send to LLM with full history and proper maxTokens
            // For STICKY_FACTS, the system message with facts is already in the history
            // For other strategies, we may need to update or add the system prompt
            val messagesToSend = if (effectiveHistory.isNotEmpty() && effectiveHistory.first().role == MessageRole.SYSTEM) {
                // If there's already a system message (e.g., from STICKY_FACTS), use it as-is
                // Otherwise, update the system prompt in the first message
                effectiveHistory
            } else {
                // Add system prompt as first message if not present
                val systemMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = effectiveSystemPrompt,
                    role = MessageRole.SYSTEM
                )
                listOf(systemMessage) + effectiveHistory
            }
            
            val result = llmClient.sendChat(messagesToSend, chatRequest.maxTokens, chatRequest.model.modelName)
            
            result.map { chatResponse ->
                // Create token metrics
                val tokenMetrics = TokenMetrics(
                    currentRequestTokens = currentRequestTokens,
                    historyTokens = historyTokens,
                    completionTokens = chatResponse.completionTokens
                )
                
                // Return the response with token metrics
                AiChatResponse(chatResponse.message, null, tokenMetrics)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sends a message with restrictions to the LLM and manages the chat history
     */
    suspend fun sendMessageWithRestrictions(
        chatRequest: ChatRequest,
        useJsonFormat: Boolean,
        limitLength: Boolean,
        useStopSequence: Boolean,
        stopSequenceText: String,
        contextStrategy: ContextStrategy = ContextStrategy.SLIDING_WINDOW,
        chatId: String? = null,
        taskContextId: String? = null
    ): Result<AiChatResponse> = withContext(dispatcher) {
        try {
            val memoryContext = buildMemoryContext(chatId, taskContextId, contextStrategy)
            val enrichedSystemPrompt = buildEnrichedSystemPrompt(chatRequest, memoryContext)
            logMemoryLayers(chatId, memoryContext)

            // Use the history provided in the chatRequest
            val effectiveHistory = buildEffectiveHistory(
                chatRequest = chatRequest,
                contextStrategy = contextStrategy,
                memoryContext = memoryContext
            )
            
            // Create the current user message with restrictions
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                content = buildUserMessageWithRestrictions(
                    originalMessage = chatRequest.message,
                    useJsonFormat = useJsonFormat,
                    limitLength = limitLength
                ),
                role = MessageRole.USER
            )
            
            // Add the current user message to the history
            effectiveHistory.add(userMessage)
            
            // Log the history size for debugging
            Log.d("ChatAgent", "Sending history size with restrictions: ${effectiveHistory.size}")
            
            // Debug logging for BRANCHING strategy
            if (contextStrategy == ContextStrategy.BRANCHING) {
                Log.d("BRANCHING_FINAL_REQUEST", "History messages (with restrictions):")
                effectiveHistory.forEachIndexed { index, message ->
                    Log.d("BRANCHING_FINAL_REQUEST", "index=$index role=${message.role} content=${message.content.take(50)}...")
                }
                Log.d("BRANCHING_FINAL_REQUEST", "currentMessage=${chatRequest.message}")
            }
            
            // Calculate token metrics before sending to LLM
            val currentRequestTokens = TokenCounter.countTokens(chatRequest.message)
            val historyTokens = effectiveHistory.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
                .sumOf { TokenCounter.countTokens(it.content) }
            
            // For sticky facts strategy, do not modify the system prompt as it's already handled by ViewModel
            // The system prompt with facts is already in the first message of effectiveHistory
            val effectiveSystemPrompt = enrichedSystemPrompt
            
            // Send to LLM with full history and proper maxTokens
            // For STICKY_FACTS, the system message with facts is already in the history
            // For other strategies, we may need to update or add the system prompt
            val messagesToSend = if (effectiveHistory.isNotEmpty() && effectiveHistory.first().role == MessageRole.SYSTEM) {
                // If there's already a system message (e.g., from STICKY_FACTS), use it as-is
                // Otherwise, update the system prompt in the first message
                effectiveHistory
            } else {
                // Add system prompt as first message if not present
                val systemMessage = Message(
                    id = UUID.randomUUID().toString(),
                    content = effectiveSystemPrompt,
                    role = MessageRole.SYSTEM
                )
                listOf(systemMessage) + effectiveHistory
            }
            
            val result = llmClient.sendChat(messagesToSend, chatRequest.maxTokens, chatRequest.model.modelName)
            
            result.map { chatResponse ->
                // Create token metrics
                val tokenMetrics = TokenMetrics(
                    currentRequestTokens = currentRequestTokens,
                    historyTokens = historyTokens,
                    completionTokens = chatResponse.completionTokens
                )
                
                // Return the response with token metrics
                AiChatResponse(chatResponse.message, null, tokenMetrics)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildUserMessageWithRestrictions(
        originalMessage: String,
        useJsonFormat: Boolean,
        limitLength: Boolean
    ): String {
        val stringBuilder = StringBuilder(originalMessage)
        
        if (useJsonFormat) {
            stringBuilder.append("\n\nReturn the answer strictly as valid JSON with this structure:\n{\n\"topic\": \"short topic\",\n\"date\": \"today's date in YYYY-MM-DD format\",\n\"time\": \"current time in HH:MM format\",\n\"answer\": \"main answer\",\n\"tags\": [\"tag1\", \"tag2\", \"tag3\"]\n}\nDo not add any text before or after JSON. For date and time fields, provide actual values, not placeholders.")
        }
        
        if (limitLength) {
            stringBuilder.append("\n\nKeep the answer short. Maximum 500 characters. Use no more than 3 sentences.")
        }
        
        return stringBuilder.toString()
    }

    private suspend fun buildMemoryContext(
        chatId: String?,
        taskContextId: String?,
        contextStrategy: ContextStrategy
    ): MemoryContext? {
        if (chatId == null) return null

        val shortTermLimit = when (contextStrategy) {
            ContextStrategy.NO_STRATEGY -> Int.MAX_VALUE
            else -> 5
        }

        return memoryOrchestrator.buildMemoryContext(
            chatId = chatId,
            taskContextId = taskContextId,
            shortTermLimit = shortTermLimit
        )
    }

    private fun buildEnrichedSystemPrompt(
        chatRequest: ChatRequest,
        memoryContext: MemoryContext?
    ): String {
        val baseSystemPrompt = chatRequest.systemPrompt.orEmpty()
        return memoryContext?.let {
            promptBuilder.buildSystemPrompt(baseSystemPrompt, it)
        } ?: baseSystemPrompt
    }

    private fun buildEffectiveHistory(
        chatRequest: ChatRequest,
        contextStrategy: ContextStrategy,
        memoryContext: MemoryContext?
    ): MutableList<Message> {
        val memoryShortTermMessages = memoryContext
            ?.shortTermMessages
            ?.withoutCurrentUserMessage(chatRequest.message)
            .orEmpty()

        val baseHistory = when {
            memoryContext == null -> chatRequest.history
            contextStrategy == ContextStrategy.STICKY_FACTS -> chatRequest.history
            contextStrategy == ContextStrategy.BRANCHING -> chatRequest.history
            else -> memoryShortTermMessages
        }

        return baseHistory.toMutableList()
    }

    private fun logMemoryLayers(chatId: String?, memoryContext: MemoryContext?) {
        Log.d("MEMORY_LAYERS", "chatId=$chatId")
        Log.d("MEMORY_LAYERS", "taskContextId=${memoryContext?.taskContext?.id}")
        Log.d("MEMORY_LAYERS", "shortTermMessages=${memoryContext?.shortTermMessages?.size ?: 0}")
        Log.d("MEMORY_LAYERS", "workingMemory=${memoryContext?.taskContext != null}")
        Log.d("MEMORY_LAYERS", "longTermMemory=${memoryContext != null}")
    }

    private fun List<Message>.withoutCurrentUserMessage(currentMessage: String): List<Message> {
        if (isEmpty()) return this
        val lastMessage = last()
        return if (lastMessage.role == MessageRole.USER && lastMessage.content == currentMessage) {
            dropLast(1)
        } else {
            this
        }
    }
}
