package com.aiassistant.core.domain.repository

interface PrivateVpsConnectionTester {
    suspend fun test(baseUrl: String, model: String, apiKey: String): Result<String>
}
