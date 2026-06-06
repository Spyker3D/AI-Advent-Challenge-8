package com.aiassistant.di

import com.aiassistant.MainActivity
import com.aiassistant.core.data.di.DataModule
import com.aiassistant.core.domain.di.DomainModule
import com.aiassistant.core.network.di.NetworkModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        NetworkModule::class,
        DataModule::class,
        DomainModule::class,
        ViewModelModule::class
    ]
)
interface AppComponent {
    fun inject(mainActivity: MainActivity)
}