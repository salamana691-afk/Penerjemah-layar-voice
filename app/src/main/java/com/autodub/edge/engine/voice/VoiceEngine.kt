package com.autodub.edge.engine.voice

import com.autodub.edge.core.util.AppResult
import com.autodub.edge.domain.model.VoiceDescriptor
import java.io.File

data class VoiceSynthesisRequest(
    val text: String,
    val voiceId: String,
    val pitch: Float,
    val speechRate: Float,
    val volume: Float,
    val languageTag: String = "id-ID",
    val customSsmlStyle: String = ""
)

interface VoiceEngine {
    val providerId: String
    val supportsSsml: Boolean
    suspend fun initialize(): AppResult<Unit>
    suspend fun isReady(): Boolean
    suspend fun getAvailableVoices(languagePrefix: String = "id"): List<VoiceDescriptor>
    suspend fun synthesizeToFile(
        request: VoiceSynthesisRequest,
        outputFile: File
    ): AppResult<File>
    fun release()
}
