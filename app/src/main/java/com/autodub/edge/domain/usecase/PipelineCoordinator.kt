package com.autodub.edge.domain.usecase

import android.graphics.Bitmap
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.network.ConnectivityObserver
import com.autodub.edge.core.util.AppError
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.core.util.TextNormalizer
import com.autodub.edge.data.preferences.PreferencesRepository
import com.autodub.edge.data.repository.CharacterRepository
import com.autodub.edge.domain.model.CaptureStatus
import com.autodub.edge.domain.model.CharacterProfile
import com.autodub.edge.domain.model.DialogueEvent
import com.autodub.edge.domain.model.DubbingSettings
import com.autodub.edge.domain.model.OcrStatus
import com.autodub.edge.domain.model.OverlayAppearanceConfig
import com.autodub.edge.domain.model.PipelineRuntimeState
import com.autodub.edge.domain.model.RoiLayoutConfig
import com.autodub.edge.domain.model.RoiMode
import com.autodub.edge.domain.model.VoiceEngineOperatingMode
import com.autodub.edge.domain.model.VoiceEngineStatus
import com.autodub.edge.engine.audio.AudioPlaybackManager
import com.autodub.edge.engine.audio.QueuedAudioTrack
import com.autodub.edge.engine.ocr.DualBoxOcrProcessor
import com.autodub.edge.engine.translation.TranslationRepository
import com.autodub.edge.engine.voice.VoiceOrchestrator
import com.autodub.edge.engine.voice.VoiceSynthesisRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class PipelineCoordinator(
    private val characterRepository: CharacterRepository,
    private val preferencesRepository: PreferencesRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val ocrProcessor: DualBoxOcrProcessor,
    private val translationRepository: TranslationRepository,
    private val voiceOrchestrator: VoiceOrchestrator,
    private val audioPlaybackManager: AudioPlaybackManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val characterMatcher = CharacterMatcher()
    private val duplicateDetector = DuplicateDialogueDetector()
    private val isProcessingFrame = AtomicBoolean(false)
    private var activeDubbingJob: Job? = null

    private val _runtimeState = MutableStateFlow(PipelineRuntimeState())
    val runtimeState: StateFlow<PipelineRuntimeState> = _runtimeState.asStateFlow()

    private val _recentDialogues = MutableStateFlow<List<DialogueEvent>>(emptyList())
    val recentDialogues: StateFlow<List<DialogueEvent>> = _recentDialogues.asStateFlow()

    val roiLayoutState: StateFlow<RoiLayoutConfig> = preferencesRepository.roiLayoutFlow
        .stateIn(scope, SharingStarted.Eagerly, RoiLayoutConfig())

    val overlayConfigState: StateFlow<OverlayAppearanceConfig> = preferencesRepository.overlayConfigFlow
        .stateIn(scope, SharingStarted.Eagerly, OverlayAppearanceConfig())

    val dubbingSettingsState: StateFlow<DubbingSettings> = preferencesRepository.dubbingSettingsFlow
        .stateIn(scope, SharingStarted.Eagerly, DubbingSettings())

    val charactersState: StateFlow<List<CharacterProfile>> = characterRepository.observeCharacters()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            characterRepository.ensureSeeded()
            voiceOrchestrator.initialize()
            _runtimeState.update { it.copy(voiceEngineStatus = VoiceEngineStatus.READY) }

            val settings = dubbingSettingsState.value
            translationRepository.checkModelAvailability(
                settings.sourceLanguageCode,
                settings.targetLanguageCode
            )
        }

        scope.launch {
            connectivityObserver.isOnline.collectLatest { online ->
                _runtimeState.update { state ->
                    state.copy(
                        isNetworkOnline = online,
                        activeVoiceProviderLabel = computeProviderStatusLabel(
                            online = online,
                            settings = dubbingSettingsState.value
                        )
                    )
                }
            }
        }

        scope.launch {
            translationRepository.modelState.collectLatest { modelState ->
                _runtimeState.update { it.copy(translationModelState = modelState) }
            }
        }

        scope.launch {
            roiLayoutState.collectLatest { roi ->
                _runtimeState.update { it.copy(roiMode = roi.mode) }
            }
        }

        scope.launch {
            overlayConfigState.collectLatest { overlay ->
                _runtimeState.update { it.copy(isTranslationVisible = overlay.isTranslationVisible) }
            }
        }

        scope.launch {
            dubbingSettingsState.collectLatest { settings ->
                _runtimeState.update {
                    it.copy(
                        isVoiceEnabled = settings.voiceEnabled,
                        voiceOperatingMode = settings.voiceOperatingMode,
                        activeVoiceProviderLabel = computeProviderStatusLabel(
                            online = connectivityObserver.isOnline.value,
                            settings = settings
                        )
                    )
                }
            }
        }

        scope.launch {
            audioPlaybackManager.state.collectLatest { audioState ->
                _runtimeState.update { current ->
                    val nextVoiceStatus = when {
                        audioState.lastError != null -> VoiceEngineStatus.ERROR
                        audioState.isPlaying -> VoiceEngineStatus.SPEAKING
                        else -> VoiceEngineStatus.READY
                    }
                    current.copy(
                        voiceEngineStatus = nextVoiceStatus,
                        queueSize = audioState.queueSize
                    )
                }
            }
        }
    }

    private fun computeProviderStatusLabel(online: Boolean, settings: DubbingSettings): String {
        return when (settings.voiceOperatingMode) {
            VoiceEngineOperatingMode.AUTOMATIC -> {
                if (online && settings.defaultProvider.isOnlineProvider) {
                    "Auto · Online (${settings.defaultProvider.id})"
                } else {
                    "Auto · Offline Fallback (Android TTS)"
                }
            }
            VoiceEngineOperatingMode.MANUAL -> "Manual · ${settings.defaultProvider.id}"
        }
    }

    fun updateCaptureStatus(status: CaptureStatus, errorMessage: String? = null) {
        _runtimeState.update {
            it.copy(
                captureStatus = status,
                ocrStatus = when (status) {
                    CaptureStatus.RUNNING -> if (it.isTranslationPaused) OcrStatus.PAUSED else OcrStatus.SCANNING
                    CaptureStatus.PAUSED -> OcrStatus.PAUSED
                    CaptureStatus.ERROR -> OcrStatus.ERROR
                    else -> OcrStatus.IDLE
                },
                lastErrorMessage = errorMessage ?: if (status == CaptureStatus.RUNNING) null else it.lastErrorMessage
            )
        }
    }

    fun updateOverlayRunning(running: Boolean) {
        _runtimeState.update { it.copy(isOverlayRunning = running) }
    }

    fun setPaused(paused: Boolean) {
        _runtimeState.update {
            it.copy(
                isTranslationPaused = paused,
                ocrStatus = when {
                    it.captureStatus != CaptureStatus.RUNNING -> OcrStatus.IDLE
                    paused -> OcrStatus.PAUSED
                    else -> OcrStatus.SCANNING
                }
            )
        }
        if (paused) {
            activeDubbingJob?.cancel()
            audioPlaybackManager.pause()
        } else {
            audioPlaybackManager.resume()
        }
    }

    fun togglePause() {
        setPaused(!_runtimeState.value.isTranslationPaused)
    }

    fun toggleTranslationVisibility() {
        val current = overlayConfigState.value
        scope.launch {
            preferencesRepository.saveOverlayConfig(
                current.copy(isTranslationVisible = !current.isTranslationVisible)
            )
        }
    }

    fun toggleVoiceEnabled() {
        val current = dubbingSettingsState.value
        val nextEnabled = !current.voiceEnabled
        if (!nextEnabled) {
            audioPlaybackManager.stopAndClear()
        }
        scope.launch {
            preferencesRepository.saveDubbingSettings(current.copy(voiceEnabled = nextEnabled))
        }
    }

    fun setRoiMode(mode: RoiMode) {
        val current = roiLayoutState.value
        scope.launch {
            preferencesRepository.saveRoiLayout(current.copy(mode = mode))
        }
    }

    suspend fun downloadTranslationModel(): AppResult<Unit> {
        val settings = dubbingSettingsState.value
        return translationRepository.ensureModelDownloaded(
            settings.sourceLanguageCode,
            settings.targetLanguageCode
        )
    }

    suspend fun processScreenBitmap(bitmap: Bitmap) {
        val state = _runtimeState.value
        if (state.isTranslationPaused || state.captureStatus != CaptureStatus.RUNNING) {
            return
        }
        if (roiLayoutState.value.mode == RoiMode.SETUP) {
            return
        }
        if (!isProcessingFrame.compareAndSet(false, true)) {
            return
        }

        try {
            val settings = dubbingSettingsState.value
            val layout = roiLayoutState.value
            val ocrResult = ocrProcessor.processFrame(
                frameBitmap = bitmap,
                layout = layout,
                minConfidenceThreshold = settings.ocrSensitivity * 0.6f
            )

            when (ocrResult) {
                is AppResult.Error -> {
                    if (ocrResult.error !is AppError.DialogNotFound) {
                        _runtimeState.update { it.copy(lastErrorMessage = ocrResult.error.userMessage) }
                    }
                    return
                }
                AppResult.Loading -> return
                is AppResult.Success -> {
                    val ocr = ocrResult.data
                    val match = characterMatcher.match(
                        rawOcrName = ocr.rawCharacterName,
                        profiles = charactersState.value
                    )

                    val isNew = duplicateDetector.isNewDialogue(
                        speakerKey = match.detectedSpeakerName,
                        dialogueText = ocr.normalizedDialogueText
                    )
                    if (!isNew) {
                        return
                    }

                    activeDubbingJob?.cancel()
                    activeDubbingJob = scope.launch {
                        handleNewDialogue(
                            speakerDisplay = match.detectedSpeakerName,
                            profile = match.profile,
                            sourceText = ocr.normalizedDialogueText,
                            settings = settings
                        )
                    }
                }
            }
        } finally {
            isProcessingFrame.set(false)
        }
    }

    private suspend fun handleNewDialogue(
        speakerDisplay: String,
        profile: CharacterProfile,
        sourceText: String,
        settings: DubbingSettings
    ) {
        try {
            if (settings.translationDelayMs > 0L) {
                delay(settings.translationDelayMs)
            }

            // Offline ML Kit Translation always runs independently of voice internet availability
            val translationResult = translationRepository.translate(
                text = sourceText,
                sourceCode = settings.sourceLanguageCode,
                targetCode = settings.targetLanguageCode
            )

            val translatedText = when (translationResult) {
                is AppResult.Success -> translationResult.data
                is AppResult.Error -> {
                    _runtimeState.update {
                        it.copy(
                            currentSpeaker = speakerDisplay,
                            currentSourceText = sourceText,
                            lastErrorMessage = translationResult.error.userMessage
                        )
                    }
                    return
                }
                AppResult.Loading -> return
            }

            _runtimeState.update {
                it.copy(
                    currentSpeaker = speakerDisplay,
                    currentSourceText = sourceText,
                    currentTranslatedText = translatedText,
                    lastErrorMessage = null
                )
            }

            val shouldDub = settings.voiceEnabled && settings.autoPlay && profile.enabled
            var providerUsed = profile.voiceProvider
            var voiceUsed = profile.voiceId
            var cacheHit = false

            if (shouldDub) {
                val effectiveVoiceId = profile.voiceId.ifBlank { settings.defaultVoiceId }
                val effectiveProvider = if (settings.voiceOperatingMode == VoiceEngineOperatingMode.MANUAL) {
                    settings.defaultProvider.id
                } else {
                    profile.voiceProvider.ifBlank { settings.defaultProvider.id }
                }
                voiceUsed = effectiveVoiceId

                val synthResult = voiceOrchestrator.synthesize(
                    preferredProvider = effectiveProvider,
                    operatingMode = settings.voiceOperatingMode,
                    enableCache = settings.enableAudioCache,
                    request = VoiceSynthesisRequest(
                        text = translatedText,
                        voiceId = effectiveVoiceId,
                        offlineVoiceId = profile.offlineVoiceId,
                        personality = profile.voicePersonality,
                        emotion = profile.emotion,
                        customVoiceDescription = profile.customVoiceDescription.ifBlank {
                            settings.defaultCustomVoiceDescription
                        },
                        pitch = profile.pitch,
                        speechRate = profile.speechRate,
                        volume = profile.volume,
                        languageTag = if (settings.targetLanguageCode == "id") "id-ID" else settings.targetLanguageCode,
                        customSsmlStyle = profile.customSsmlStyle
                    )
                )

                when (synthResult) {
                    is AppResult.Success -> {
                        providerUsed = synthResult.data.providerUsed
                        cacheHit = synthResult.data.fromCache
                        _runtimeState.update {
                            it.copy(
                                voiceEngineStatus = if (synthResult.data.usedFallback) {
                                    VoiceEngineStatus.FALLBACK_ACTIVE
                                } else {
                                    VoiceEngineStatus.SPEAKING
                                },
                                activeVoiceProviderLabel = buildString {
                                    append(providerUsed)
                                    if (synthResult.data.usedFallback) append(" (Fallback)")
                                    if (cacheHit) append(" · Cache")
                                },
                                lastCacheHit = cacheHit
                            )
                        }
                        audioPlaybackManager.enqueue(
                            track = QueuedAudioTrack(
                                id = UUID.randomUUID().toString(),
                                file = synthResult.data.audioFile,
                                speakerName = speakerDisplay,
                                volume = (profile.volume * settings.defaultVolume).coerceIn(0f, 1f),
                                speed = 1.0f
                            ),
                            policy = settings.audioQueuePolicy
                        )
                    }
                    is AppResult.Error -> {
                        _runtimeState.update {
                            it.copy(
                                voiceEngineStatus = VoiceEngineStatus.ERROR,
                                lastErrorMessage = synthResult.error.userMessage
                            )
                        }
                    }
                    AppResult.Loading -> Unit
                }
            }

            val event = DialogueEvent(
                speakerName = speakerDisplay,
                matchedProfileName = profile.characterName,
                sourceText = sourceText,
                translatedText = translatedText,
                voiceIdUsed = voiceUsed.ifBlank { "System Default" },
                providerUsed = providerUsed,
                emotionUsed = profile.emotion.displayName,
                cacheHit = cacheHit
            )
            _recentDialogues.update { list -> (listOf(event) + list).take(25) }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            _runtimeState.update {
                it.copy(lastErrorMessage = t.message ?: "Kesalahan pipeline terjemahan.")
            }
        }
    }

    suspend fun testCharacterVoice(profile: CharacterProfile, sampleText: String): AppResult<Unit> {
        val settings = dubbingSettingsState.value
        val provider = if (settings.voiceOperatingMode == VoiceEngineOperatingMode.MANUAL) {
            settings.defaultProvider.id
        } else {
            profile.voiceProvider.ifBlank { settings.defaultProvider.id }
        }
        val voiceId = profile.voiceId.ifBlank { settings.defaultVoiceId }
        val result = voiceOrchestrator.synthesize(
            preferredProvider = provider,
            operatingMode = settings.voiceOperatingMode,
            enableCache = settings.enableAudioCache,
            request = VoiceSynthesisRequest(
                text = sampleText,
                voiceId = voiceId,
                offlineVoiceId = profile.offlineVoiceId,
                personality = profile.voicePersonality,
                emotion = profile.emotion,
                customVoiceDescription = profile.customVoiceDescription.ifBlank {
                    settings.defaultCustomVoiceDescription
                },
                pitch = profile.pitch,
                speechRate = profile.speechRate,
                volume = profile.volume,
                languageTag = if (settings.targetLanguageCode == "id") "id-ID" else settings.targetLanguageCode,
                customSsmlStyle = profile.customSsmlStyle
            )
        )
        return when (result) {
            is AppResult.Success -> {
                _runtimeState.update {
                    it.copy(
                        activeVoiceProviderLabel = if (result.data.usedFallback) {
                            "${result.data.providerUsed} (Fallback)"
                        } else {
                            result.data.providerUsed
                        },
                        lastCacheHit = result.data.fromCache
                    )
                }
                audioPlaybackManager.enqueue(
                    track = QueuedAudioTrack(
                        id = UUID.randomUUID().toString(),
                        file = result.data.audioFile,
                        speakerName = formatSpeaker(profile.characterName),
                        volume = profile.volume,
                        speed = 1.0f
                    ),
                    policy = com.autodub.edge.domain.model.AudioQueuePolicy.REPLACE_CURRENT
                )
                AppResult.Success(Unit)
            }
            is AppResult.Error -> result
            AppResult.Loading -> AppResult.Loading
        }
    }

    private fun formatSpeaker(name: String): String {
        return if (name == AppConstants.DEFAULT_NARRATOR_NAME) {
            AppConstants.DEFAULT_NARRATOR_DISPLAY
        } else {
            TextNormalizer.toDisplayName(name)
        }
    }

    fun resetSessionCaches() {
        activeDubbingJob?.cancel()
        characterMatcher.resetSession()
        duplicateDetector.reset()
        audioPlaybackManager.stopAndClear()
        _runtimeState.update {
            it.copy(
                currentSpeaker = AppConstants.DEFAULT_NARRATOR_DISPLAY,
                currentSourceText = "",
                currentTranslatedText = "",
                lastErrorMessage = null
            )
        }
    }

    fun release() {
        activeDubbingJob?.cancel()
        connectivityObserver.release()
        ocrProcessor.close()
        translationRepository.close()
        voiceOrchestrator.release()
        audioPlaybackManager.release()
    }
}
