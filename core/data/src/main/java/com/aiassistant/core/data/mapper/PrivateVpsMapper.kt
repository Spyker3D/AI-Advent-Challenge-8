package com.aiassistant.core.data.mapper

import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.network.dto.PrivateVpsChatRequestDto
import com.aiassistant.core.network.dto.PrivateVpsMessageDto

fun ChatSettings.privateVpsEndpoint(path: String): String? =
    ChatSettings.normalizePrivateVpsBaseUrl(privateVpsBaseUrl)?.let { it + path.removePrefix("/") }

fun buildPrivateVpsRequest(
    messages: List<Message>,
    settings: ChatSettings,
    maxTokens: Int? = null
): PrivateVpsChatRequestDto {
    val system = messages.filter { it.role == MessageRole.SYSTEM && it.content.isNotBlank() }
        .joinToString("\n\n") { it.content.trim() }
    val mapped = buildList {
        if (system.isNotBlank()) add(PrivateVpsMessageDto("system", system))
        messages.filter { it.role != MessageRole.SYSTEM && it.content.isNotBlank() }
            .takeLast(30).forEach {
                add(PrivateVpsMessageDto(if (it.role == MessageRole.ASSISTANT) "assistant" else "user", it.content.trim()))
            }
    }
    return PrivateVpsChatRequestDto(
        model = settings.privateVpsModel.trim(),
        messages = mapped,
        temperature = settings.localTemperature.toDouble(),
        maxTokens = ChatSettings.safeLocalMaxTokens(maxTokens ?: settings.localMaxTokens),
        stream = false
    )
}
