package com.autodub.edge.core.di

import android.content.Context
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.network.NetworkConnectivityMonitor
import com.autodub.edge.data.database.CharacterDatabase
import com.autodub.edge.data.preferences.PreferencesRepository
import com.autodub.edge.data.repository.CharacterRepository
import com.autodub.edge.domain.usecase.PipelineCoordinator
import com.autodub.edge.engine.audio.AudioPlaybackManager
import com.autodub.edge.engine.audio.TtsAudioCache
import com.autodub.edge.engine.ocr.DualBoxOcrProcessor
import com.autodub.edge.engine.translation.MlKitTranslationRepository
import com.autodub.edge.engine.translation.TranslationRepository
import com.autodub.edge.engine.voice.AndroidNativeVoiceEngine
import com.autodub.edge.engine.voice.EdgeCompatibleVoiceEngine
import com.autodub.edge.engine.voice.GeminiExpressiveVoiceEngine
import com.autodub.edge.engine.voice.VoiceOrchestrator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

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

    val connectivityMonitor: NetworkConnectivityMonitor by lazy {
        NetworkConnectivityMonitor(appContext)
    }

    val ttsAudioCache: TtsAudioCache by lazy {
        TtsAudioCache(File(appContext.cacheDir, AppConstants.TTS_CACHE_DIR))
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

    val geminiVoiceEngine: GeminiExpressiveVoiceEngine by lazy {
        GeminiExpressiveVoiceEngine(
            modelProvider = {
                runBlocking { preferencesRepository.dubbingSettingsFlow.first().geminiModelId }
            },
            endpointOverrideProvider = {
                runBlocking { preferencesRepository.dubbingSettingsFlow.first().geminiEndpointUrl }
            },
            apiKeyProvider = {
                runBlocking { preferencesRepository.dubbingSettingsFlow.first().geminiApiKey }
            }
        )
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
            geminiEngine = geminiVoiceEngine,
            edgeEngine = edgeVoiceEngine,
            connectivityObserver = connectivityMonitor,
            audioCache = ttsAudioCache,
            modelIdProvider = {
                runBlocking { preferencesRepository.dubbingSettingsFlow.first().geminiModelId }
            }
        )
    }

    val audioPlaybackManager: AudioPlaybackManager by lazy {
        AudioPlaybackManager(appContext)
    }

    val pipelineCoordinator: PipelineCoordinator by lazy {
        PipelineCoordinator(
            characterRepository = characterRepository,
            preferencesRepository = preferencesRepository,
            connectivityObserver = connectivityMonitor,
            ocrProcessor = ocrProcessor,
            translationRepository = translationRepository,
            voiceOrchestrator = voiceOrchestrator,
            audioPlaybackManager = audioPlaybackManager
        )
    }
}
