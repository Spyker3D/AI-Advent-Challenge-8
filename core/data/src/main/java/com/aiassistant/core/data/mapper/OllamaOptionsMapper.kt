package com.aiassistant.core.data.mapper

import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.network.dto.OllamaOptionsDto

fun ChatSettings.toOllamaOptionsDto() = OllamaOptionsDto(
    temperature = ChatSettings.safeLocalTemperature(localTemperature).toDouble(),
    numPredict = ChatSettings.safeLocalMaxTokens(localMaxTokens),
    numCtx = ChatSettings.safeLocalContextWindow(localContextWindow),
    topP = ChatSettings.safeLocalTopP(localTopP).toDouble(),
    repeatPenalty = ChatSettings.safeLocalRepeatPenalty(localRepeatPenalty).toDouble(),
    seed = localSeed
)
