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


}
