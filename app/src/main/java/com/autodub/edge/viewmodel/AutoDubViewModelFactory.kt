package com.autodub.edge.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autodub.edge.core.di.AppContainer

class AutoDubViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(
                    coordinator = container.pipelineCoordinator,
                    preferencesRepository = container.preferencesRepository,
                    voiceOrchestrator = container.voiceOrchestrator,
                    audioPlaybackManager = container.audioPlaybackManager
                ) as T
            }
            modelClass.isAssignableFrom(CharacterViewModel::class.java) -> {
                CharacterViewModel(
                    characterRepository = container.characterRepository,
                    voiceOrchestrator = container.voiceOrchestrator,
                    coordinator = container.pipelineCoordinator
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
