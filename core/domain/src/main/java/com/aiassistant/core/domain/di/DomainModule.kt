package com.aiassistant.core.domain.di

import dagger.Binds
import dagger.Module
import com.aiassistant.core.domain.support.DefaultSupportAssistantService
import com.aiassistant.core.domain.support.SupportAssistantService

@Module(includes = [AgentModule::class])
abstract class DomainModule {
    // UseCase классы автоматически инжектируются через @Inject конструктор
    @Binds abstract fun bindSupportAssistantService(impl: DefaultSupportAssistantService): SupportAssistantService
}
