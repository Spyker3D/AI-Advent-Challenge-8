package com.aiassistant.feature.chat.voice

import dagger.Binds
import dagger.Module

@Module
abstract class VoiceInputModule {
    @Binds
    abstract fun bindVoiceRecognizer(implementation: AndroidVoiceRecognizer): VoiceRecognizer
}
