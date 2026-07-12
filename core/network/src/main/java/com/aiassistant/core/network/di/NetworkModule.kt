package com.aiassistant.core.network.di

import com.aiassistant.core.network.api.OpenAiApi
import com.aiassistant.core.network.api.PrivateVpsApi
import com.aiassistant.core.network.interceptor.PrivateVpsAuthInterceptor
import com.aiassistant.core.network.interceptor.OpenAiAuthInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
class NetworkModule {

    companion object {
        private const val OPENAI_BASE_URL = "https://api.openai.com/"
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }
    }

    @Provides
    @Singleton
    fun provideDefaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @Named("OpenAiOkHttp")
    fun provideOpenAiOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        openAiAuthInterceptor: OpenAiAuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(openAiAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("OpenAiRetrofit")
    fun provideOpenAiRetrofit(
        @Named("OpenAiOkHttp") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OPENAI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAiApi(@Named("OpenAiRetrofit") retrofit: Retrofit): OpenAiApi {
        return retrofit.create(OpenAiApi::class.java)
    }

    @Provides
    @Singleton
    @PrivateVpsClient
    fun providePrivateVpsOkHttpClient(auth: PrivateVpsAuthInterceptor): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logger)
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .callTimeout(330, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @PrivateVpsRetrofit
    fun providePrivateVpsRetrofit(@PrivateVpsClient client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder().baseUrl("https://localhost/").client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

    @Provides
    @Singleton
    fun providePrivateVpsApi(@PrivateVpsRetrofit retrofit: Retrofit): PrivateVpsApi =
        retrofit.create(PrivateVpsApi::class.java)
}
