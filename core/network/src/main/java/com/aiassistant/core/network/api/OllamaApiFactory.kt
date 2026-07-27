package com.aiassistant.core.network.api

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OllamaApiFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    fun create(baseUrl: String): OllamaApi {
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(OllamaApi::class.java)
    }

    fun normalizeBaseUrl(url: String): String {
        val normalized = url.trim().trimEnd('/')
        val uri = runCatching { java.net.URI(normalized) }.getOrNull()
        require(uri?.scheme in setOf("http", "https") && !uri?.host.isNullOrBlank()) {
            "Invalid Ollama Base URL"
        }
        return "$normalized/"
    }
}
