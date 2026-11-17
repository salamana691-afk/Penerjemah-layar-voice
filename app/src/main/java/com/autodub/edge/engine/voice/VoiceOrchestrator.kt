package com.autodub.edge.engine.voice

import android.content.Context
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.domain.model.VoiceDescriptor
import java.io.File
import java.util.UUID

data class SynthesizedAudio(
    val audioFile: File,
    val providerUsed: String,
    val usedFallback: Boolean
)

class VoiceOrchestrator(
    private val context: Context,
    private val androidEngine: AndroidNativeVoiceEngine,
    private val edgeEngine: EdgeCompatibleVoiceEngine
) {
    private val tempDir: File by lazy {
        File(context.cacheDir, "tts_temp").apply { mkdirs() }
    }

    suspend fun initialize() {
        androidEngine.initialize()
        edgeEngine.initialize()
    }

    suspend fun getVoicesForProvider(providerId: String): List<VoiceDescriptor> {
        return when (providerId) {
            AppConstants.PROVIDER_EDGE_COMPATIBLE -> edgeEngine.getAvailableVoices("id")
            else -> androidEngine.getAvailableVoices("id")
        }
    }

    suspend fun synthesize(
        preferredProvider: String,
        request: VoiceSynthesisRequest
    ): AppResult<SynthesizedAudio> {
        val useEdge = preferredProvider == AppConstants.PROVIDER_EDGE_COMPATIBLE && edgeEngine.isReady()

        if (useEdge) {
            val mp3File = File(tempDir, "dub_${UUID.randomUUID()}.mp3")
            val edgeResult = edgeEngine.synthesizeToFile(request, mp3File)
            if (edgeResult is AppResult.Success) {
                return AppResult.Success(
                    SynthesizedAudio(
                        audioFile = edgeResult.data,
                        providerUsed = edgeEngine.providerId,
                        usedFallback = false
                    )
                )
            } else {
                if (mp3File.exists()) mp3File.delete()
            }
        }

        val wavFile = File(tempDir, "dub_${UUID.randomUUID()}.wav")
        val fallbackResult = androidEngine.synthesizeToFile(request, wavFile)
        return when (fallbackResult) {
            is AppResult.Success -> AppResult.Success(
                SynthesizedAudio(
                    audioFile = fallbackResult.data,
                    providerUsed = androidEngine.providerId,
                    usedFallback = useEdge
                )
            )
            is AppResult.Error -> {
                if (wavFile.exists()) wavFile.delete()
                fallbackResult
            }
            AppResult.Loading -> AppResult.Loading
        }
    }

    fun cleanupStaleTempFiles() {
        runCatching {
            tempDir.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
        }
    }

    fun release() {
        androidEngine.release()
        edgeEngine.release()
        cleanupStaleTempFiles()
    }
}
