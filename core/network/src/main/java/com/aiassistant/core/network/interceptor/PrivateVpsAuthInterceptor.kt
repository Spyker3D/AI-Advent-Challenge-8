package com.aiassistant.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivateVpsCredentials @Inject constructor() {
    @Volatile var apiKey: String = ""
}

class PrivateVpsAuthInterceptor @Inject constructor(
    private val credentials: PrivateVpsCredentials
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val key = credentials.apiKey.trim()
        check(key.isNotBlank()) { "Private VPS API key is not configured" }
        return chain.proceed(chain.request().newBuilder()
            .header("Authorization", "Bearer $key")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build())
    }
}
