package com.aiassistant

import android.app.Application
import com.aiassistant.di.AppComponent
import com.aiassistant.di.AppModule
import com.aiassistant.di.DaggerAppComponent

class AIAssistantApplication : Application() {

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AIAssistantApplication
            private set
    }
}