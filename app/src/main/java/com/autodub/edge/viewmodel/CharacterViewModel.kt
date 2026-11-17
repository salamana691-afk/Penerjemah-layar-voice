package com.autodub.edge.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.data.repository.CharacterRepository
import com.autodub.edge.domain.model.CharacterProfile
import com.autodub.edge.domain.model.VoiceDescriptor
import com.autodub.edge.domain.usecase.PipelineCoordinator
import com.autodub.edge.engine.voice.SSMLGenerator
import com.autodub.edge.engine.voice.SsmlConfig
import com.autodub.edge.engine.voice.VoiceOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CharacterViewModel(
    private val characterRepository: CharacterRepository,
    private val voiceOrchestrator: VoiceOrchestrator,
    private val coordinator: PipelineCoordinator
) : ViewModel() {

    val characters: StateFlow<List<CharacterProfile>> = characterRepository.observeCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _providerVoices = MutableStateFlow<List<VoiceDescriptor>>(emptyList())
    val providerVoices: StateFlow<List<VoiceDescriptor>> = _providerVoices.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadVoices(AppConstants.PROVIDER_ANDROID_TTS)
    }

    fun loadVoices(providerId: String) {
        viewModelScope.launch {
            _providerVoices.value = voiceOrchestrator.getVoicesForProvider(providerId)
        }
    }

    fun saveCharacter(profile: CharacterProfile) {
        viewModelScope.launch {
            when (val res = characterRepository.saveCharacter(profile)) {
                is AppResult.Success -> {
                    _statusMessage.value = "Profil suara '${profile.characterName}' tersimpan."
                }
                is AppResult.Error -> {
                    _statusMessage.value = res.error.userMessage
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            when (val res = characterRepository.deleteCharacter(id)) {
                is AppResult.Success -> {
                    _statusMessage.value = "Karakter dihapus."
                }
                is AppResult.Error -> {
                    _statusMessage.value = res.error.userMessage
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun testVoice(profile: CharacterProfile) {
        viewModelScope.launch {
            val sample = "Halo, saya ${profile.characterName}. Ini adalah suara dubbing karakter saya."
            when (val res = coordinator.testCharacterVoice(profile, sample)) {
                is AppResult.Success -> {
                    _statusMessage.value = "Memutar suara ${profile.characterName}..."
                }
                is AppResult.Error -> {
                    _statusMessage.value = res.error.userMessage
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun buildSsmlPreview(profile: CharacterProfile, sampleText: String): String {
        return SSMLGenerator.generate(
            text = sampleText,
            config = SsmlConfig(
                voiceId = profile.voiceId.ifBlank { "id-ID-GadisNeural" },
                pitchMultiplier = profile.pitch,
                rateMultiplier = profile.speechRate,
                languageTag = "id-ID",
                style = profile.customSsmlStyle
            )
        )
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}
