package com.aiassistant.core.network.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PrivateVpsClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PrivateVpsRetrofit
