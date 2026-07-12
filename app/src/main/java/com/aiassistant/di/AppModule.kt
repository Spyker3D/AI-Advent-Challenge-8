package com.aiassistant.di

import android.content.Context
import com.aiassistant.BuildConfig
import com.aiassistant.core.data.config.ApiConfig
import com.aiassistant.core.network.interceptor.OpenAiAuthInterceptor
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val context: Context) {

    @Provides
    @Singleton
    fun provideContext(): Context = context
    
    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig {
        return ApiConfig(
            openAiApiKey = BuildConfig.OPENAI_API_KEY,
            privateVpsBaseUrl = BuildConfig.PRIVATE_VPS_BASE_URL,
            privateVpsApiKey = BuildConfig.PRIVATE_VPS_API_KEY,
            privateVpsModel = BuildConfig.PRIVATE_VPS_MODEL
        )
    }

    @Provides
    @Singleton
    fun provideOpenAiAuthInterceptor(): OpenAiAuthInterceptor =
        OpenAiAuthInterceptor(BuildConfig.OPENAI_API_KEY)
}
