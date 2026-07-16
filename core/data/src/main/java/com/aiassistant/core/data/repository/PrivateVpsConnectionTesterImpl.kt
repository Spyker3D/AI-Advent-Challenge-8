package com.aiassistant.core.data.repository

import com.aiassistant.core.data.mapper.privateVpsHttpError
import com.aiassistant.core.domain.entity.ChatSettings
import com.aiassistant.core.domain.repository.PrivateVpsConnectionTester
import com.aiassistant.core.network.api.PrivateVpsApi
import com.aiassistant.core.network.interceptor.PrivateVpsCredentials
import retrofit2.HttpException
import javax.inject.Inject

class PrivateVpsConnectionTesterImpl @Inject constructor(
    private val api: PrivateVpsApi,
    private val credentials: PrivateVpsCredentials
) : PrivateVpsConnectionTester {
    override suspend fun test(baseUrl: String, model: String, apiKey: String): Result<String> = runCatching {
        val normalized = ChatSettings.normalizePrivateVpsBaseUrl(baseUrl)
            ?: error("VPS URL не настроен или некорректен.")
        require(apiKey.isNotBlank()) { "API key приватного VPS не настроен." }
        credentials.apiKey = apiKey
        val response = try { api.getModels(normalized + "api/models") }
        catch (e: HttpException) { error(privateVpsHttpError(e.code())) }
        val found = response.data.orEmpty().any { it.id == model || it.name == model }
        if (!found) error("VPS is available, but model $model was not found.")
        "VPS is available\nModel: $model"
    }
}
