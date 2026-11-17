package com.autodub.edge.core.di

import android.content.Context
import com.autodub.edge.data.database.CharacterDatabase
import com.autodub.edge.data.preferences.PreferencesRepository
import com.autodub.edge.data.repository.CharacterRepository
import com.autodub.edge.domain.usecase.PipelineCoordinator
import com.autodub.edge.engine.audio.AudioPlaybackManager
import com.autodub.edge.engine.ocr.DualBoxOcrProcessor
import com.autodub.edge.engine.translation.MlKitTranslationRepository
import com.autodub.edge.engine.translation.TranslationRepository
import com.autodub.edge.engine.voice.AndroidNativeVoiceEngine
import com.autodub.edge.engine.voice.EdgeCompatibleVoiceEngine
import com.autodub.edge.engine.voice.VoiceOrchestrator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: CharacterDatabase by lazy {
        CharacterDatabase.build(appContext)
    }

    val characterRepository: CharacterRepository by lazy {
        CharacterRepository(database.characterDao())
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(appContext)
    }

    val ocrProcessor: DualBoxOcrProcessor by lazy {
        DualBoxOcrProcessor()
    }

    val translationRepository: TranslationRepository by lazy {
        MlKitTranslationRepository()
    }

    val androidVoiceEngine: AndroidNativeVoiceEngine by lazy {
        AndroidNativeVoiceEngine(appContext)
    }

    val edgeVoiceEngine: EdgeCompatibleVoiceEngine by lazy {
        EdgeCompatibleVoiceEngine(
            endpointProvider = {
                runBlocking { preferencesRepository.dubbingSettingsFlow.first().edgeEndpointUrl }
            },
            tokenProvider = {
                runBlocking { preferencesRepository.dubbingSettingsFlow.first().edgeBearerToken }
            }
        )
    }

    val voiceOrchestrator: VoiceOrchestrator by lazy {
        VoiceOrchestrator(
            context = appContext,
            androidEngine = androidVoiceEngine,
            edgeEngine = edgeVoiceEngine
        )
    }

    val audioPlaybackManager: AudioPlaybackManager by lazy {
        AudioPlaybackManager(appContext)
    }

    val pipelineCoordinator: PipelineCoordinator by lazy {
        PipelineCoordinator(
            characterRepository = characterRepository,
            preferencesRepository = preferencesRepository,
            ocrProcessor = ocrProcessor,
            translationRepository = translationRepository,
            voiceOrchestrator = voiceOrchestrator,
            audioPlaybackManager = audioPlaybackManager
        )
    }
}
