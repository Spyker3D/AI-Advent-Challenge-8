package com.aiassistant.core.domain.di

import dagger.Module

@Module(includes = [AgentModule::class])
class DomainModule {
    // UseCase классы автоматически инжектируются через @Inject конструктор
}
