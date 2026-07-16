package com.aiassistant.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class OpenAiAuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        check(apiKey.isNotBlank()) { "OpenAI API key is not configured" }
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()
        return chain.proceed(request)
    }
}
