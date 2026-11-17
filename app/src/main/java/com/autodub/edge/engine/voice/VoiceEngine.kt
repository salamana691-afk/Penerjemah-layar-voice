package com.autodub.edge.engine.voice

import com.autodub.edge.core.util.AppResult
import com.autodub.edge.domain.model.VoiceDescriptor
import com.autodub.edge.domain.model.VoiceEmotion
import com.autodub.edge.domain.model.VoicePersonality
import java.io.File

data class VoiceSynthesisRequest(
    val text: String,
    val voiceId: String,
    val offlineVoiceId: String = "",
    val personality: VoicePersonality = VoicePersonality.ANIME_FEMALE_GENTLE,
    val emotion: VoiceEmotion = VoiceEmotion.NEUTRAL,
    val customVoiceDescription: String = "",
    val pitch: Float,
    val speechRate: Float,
    val volume: Float,
    val languageTag: String = "id-ID",
    val customSsmlStyle: String = ""
)

interface VoiceEngine {
    val providerId: String
    val supportsSsml: Boolean
    val supportsExpressiveDesign: Boolean get() = false
    suspend fun initialize(): AppResult<Unit>
    suspend fun isReady(): Boolean
    suspend fun getAvailableVoices(languagePrefix: String = "id"): List<VoiceDescriptor>
    suspend fun synthesizeToFile(
        request: VoiceSynthesisRequest,
        outputFile: File
    ): AppResult<File>
    fun release()
}
