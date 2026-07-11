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
        
        // Add conversation summary if context compression is enabled
        if (chatRequest.history.isNotEmpty() && chatRequest.history.firstOrNull()?.content?.startsWith("Conversation Summary:") == true) {
            // Add the summary message
            messages.add(
                MessageDto(
                    role = MessageRole.SYSTEM.value,
                    content = chatRequest.history.first().content
                )
            )
            
            // Add the rest of the messages (last N messages + current user message)
            chatRequest.history.drop(1).forEach { message ->
                messages.add(
                    MessageDto(
                        role = message.role.value,
                        content = message.content
                    )
                )
            }
        } else {
            // Add history messages
            chatRequest.history.forEach { message ->
                messages.add(
                    MessageDto(
                        role = message.role.value,
                        content = message.content
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
        }
        
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
            model = "gpt-4.1-mini", // Default model
            messages = messageDtos,
            temperature = 0.7f,
            maxTokens = 1000
        )
    }


}
