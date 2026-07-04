package com.aiassistant.di

import android.content.Context
import com.aiassistant.BuildConfig
import com.aiassistant.core.data.config.ApiConfig
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
            openRouterApiKey = BuildConfig.OPENROUTER_API_KEY
        )
    }
}