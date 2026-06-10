package com.aiassistant.core.data.mapper

import com.aiassistant.core.network.dto.ChatRequestDto
import com.aiassistant.core.network.dto.MessageDto
import com.aiassistant.core.domain.entity.ChatRequest
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import javax.inject.Inject

class ChatMapper @Inject constructor() {
    
    fun mapToChatRequestDto(chatRequest: ChatRequest): ChatRequestDto {
        val messages = mutableListOf<MessageDto>()
        
        // Add system message if provided
        chatRequest.systemPrompt?.let { systemPrompt ->
            messages.add(
                MessageDto(
                    role = MessageRole.SYSTEM.value,
                    content = systemPrompt
                )
            )
        }
        
        // Add user message
        messages.add(
            MessageDto(
                role = MessageRole.USER.value,
                content = chatRequest.message
            )
        )
        
        return ChatRequestDto(
            model = chatRequest.model.modelName,
            messages = messages,
            temperature = chatRequest.temperature,
            maxTokens = chatRequest.maxTokens
        )
    }

    fun mapToChatRequestDtoWithRestrictions(
        chatRequest: ChatRequest,
        useJsonFormat: Boolean,
        limitLength: Boolean,
        useStopSequence: Boolean,
        stopSequenceText: String
    ): ChatRequestDto {
        val messages = mutableListOf<MessageDto>()
        
        // Add system message if provided
        chatRequest.systemPrompt?.let { systemPrompt ->
            messages.add(
                MessageDto(
                    role = MessageRole.SYSTEM.value,
                    content = systemPrompt
                )
            )
        }
        
        // Build the user message with restrictions
        val userMessage = buildUserMessageWithRestrictions(
            originalMessage = chatRequest.message,
            useJsonFormat = useJsonFormat,
            limitLength = limitLength
        )
        
        // Add user message with restrictions
        messages.add(
            MessageDto(
                role = MessageRole.USER.value,
                content = userMessage
            )
        )
        
        // Set max tokens if limiting length
        val maxTokens = if (limitLength) 250 else chatRequest.maxTokens
        
        // Set stop sequence if enabled and text is not blank
        val stop = if (useStopSequence && stopSequenceText.isNotBlank()) {
            listOf(stopSequenceText)
        } else {
            null
        }
        
        return ChatRequestDto(
            model = chatRequest.model.modelName,
            messages = messages,
            temperature = chatRequest.temperature,
            maxTokens = maxTokens,
            stop = stop
        )
    }
    
    fun mapMessagesToChatRequestDto(messages: List<Message>): ChatRequestDto {
        val messageDtos = messages.map { message ->
            MessageDto(
                role = message.role.value,
                content = message.content
            )
        }
        
        // For this simple implementation, we'll use default values
        // In a full implementation, these would come from settings or context
        return ChatRequestDto(
            model = "gpt-4o-mini", // Default model
            messages = messageDtos,
            temperature = 0.7f,
            maxTokens = 1000
        )
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
}
