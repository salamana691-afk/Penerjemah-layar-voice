package com.autodub.edge.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.data.preferences.PreferencesRepository
import com.autodub.edge.domain.model.CharacterProfile
import com.autodub.edge.domain.model.DialogueEvent
import com.autodub.edge.domain.model.DubbingSettings
import com.autodub.edge.domain.model.OverlayAppearanceConfig
import com.autodub.edge.domain.model.PipelineRuntimeState
import com.autodub.edge.domain.model.RoiLayoutConfig
import com.autodub.edge.domain.model.RoiMode
import com.autodub.edge.domain.model.VoiceDescriptor
import com.autodub.edge.domain.usecase.PipelineCoordinator
import com.autodub.edge.engine.audio.AudioPlaybackManager
import com.autodub.edge.engine.voice.VoiceOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val coordinator: PipelineCoordinator,
    private val preferencesRepository: PreferencesRepository,
    private val voiceOrchestrator: VoiceOrchestrator,
    private val audioPlaybackManager: AudioPlaybackManager
) : ViewModel() {

    val runtimeState: StateFlow<PipelineRuntimeState> = coordinator.runtimeState
    val roiLayoutState: StateFlow<RoiLayoutConfig> = coordinator.roiLayoutState
    val overlayConfigState: StateFlow<OverlayAppearanceConfig> = coordinator.overlayConfigState
    val dubbingSettingsState: StateFlow<DubbingSettings> = coordinator.dubbingSettingsState
    val recentDialogues: StateFlow<List<DialogueEvent>> = coordinator.recentDialogues

    private val _availableVoices = MutableStateFlow<List<VoiceDescriptor>>(emptyList())
    val availableVoices: StateFlow<List<VoiceDescriptor>> = _availableVoices.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        loadVoicesForProvider(dubbingSettingsState.value.defaultProvider.id)
        refreshCacheSize()
    }

    fun loadVoicesForProvider(providerId: String) {
        viewModelScope.launch {
            _availableVoices.value = voiceOrchestrator.getVoicesForProvider(providerId)
        }
    }

    fun refreshCacheSize() {
        _cacheSizeBytes.value = voiceOrchestrator.getCacheSizeBytes()
    }

    fun clearAudioCache() {
        voiceOrchestrator.clearAudioCache()
        refreshCacheSize()
        _feedbackMessage.value = "Cache audio TTS dibersihkan."
    }

    fun saveRoiLayout(config: RoiLayoutConfig) {
        viewModelScope.launch {
            preferencesRepository.saveRoiLayout(config)
            _feedbackMessage.value = "Tata letak Dual-Box ROI disimpan."
        }
    }

    fun resetRoiLayout() {
        viewModelScope.launch {
            preferencesRepository.resetDefaultRoiLayout()
            _feedbackMessage.value = "Tata letak ROI dipulihkan ke default."
        }
    }

    fun setRoiMode(mode: RoiMode) {
        coordinator.setRoiMode(mode)
    }

    fun saveOverlayConfig(config: OverlayAppearanceConfig) {
        viewModelScope.launch {
            preferencesRepository.saveOverlayConfig(config)
        }
    }

    fun saveDubbingSettings(settings: DubbingSettings) {
        viewModelScope.launch {
            preferencesRepository.saveDubbingSettings(settings)
            audioPlaybackManager.updateVolume(settings.defaultVolume)
        }
    }

    fun downloadOfflineModel() {
        viewModelScope.launch {
            when (val res = coordinator.downloadTranslationModel()) {
                is AppResult.Success -> {
                    _feedbackMessage.value = "Model terjemahan offline siap digunakan."
                }
                is AppResult.Error -> {
                    _feedbackMessage.value = res.error.userMessage
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun testGlobalVoice() {
        val settings = dubbingSettingsState.value
        val sampleProfile = CharacterProfile(
            characterName = AppConstants.DEFAULT_NARRATOR_NAME,
            voiceProvider = settings.defaultProvider.id,
            voiceId = settings.defaultVoiceId,
            voicePersonality = settings.defaultPersonality,
            emotion = settings.defaultEmotion,
            customVoiceDescription = settings.defaultCustomVoiceDescription,
            pitch = settings.defaultPitch,
            speechRate = settings.defaultSpeechRate,
            volume = settings.defaultVolume,
            enabled = true
        )
        viewModelScope.launch {
            when (val res = coordinator.testCharacterVoice(
                sampleProfile,
                "Halo, ini adalah uji coba suara ekspresif AutoDub Edge dalam bahasa Indonesia."
            )) {
                is AppResult.Success -> {
                    refreshCacheSize()
                    _feedbackMessage.value = "Memutar pratinjau suara..."
                }
                is AppResult.Error -> _feedbackMessage.value = res.error.userMessage
                AppResult.Loading -> Unit
            }
        }
    }

    fun skipAudio() {
        audioPlaybackManager.skipCurrent()
    }

    fun togglePause() {
        coordinator.togglePause()
    }

    fun resetSession() {
        coordinator.resetSessionCaches()
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
