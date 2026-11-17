package com.autodub.edge.engine.voice

import android.content.Context
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.network.ConnectivityObserver
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.domain.model.VoiceDescriptor
import com.autodub.edge.domain.model.VoiceEngineOperatingMode
import com.autodub.edge.engine.audio.TtsAudioCache
import java.io.File
import java.util.UUID

data class SynthesizedAudio(
    val audioFile: File,
    val providerUsed: String,
    val usedFallback: Boolean,
    val fromCache: Boolean = false
)

class VoiceOrchestrator(
    private val context: Context,
    private val androidEngine: VoiceEngine,
    private val geminiEngine: VoiceEngine,
    private val edgeEngine: VoiceEngine,
    private val connectivityObserver: ConnectivityObserver,
    private val audioCache: TtsAudioCache,
    private val modelIdProvider: () -> String = { AppConstants.DEFAULT_GEMINI_TTS_MODEL },
    private val tempDirOverride: File? = null
) {
    private val tempDir: File by lazy {
        (tempDirOverride ?: File(context.cacheDir, AppConstants.TTS_TEMP_DIR)).apply { mkdirs() }
    }

    suspend fun initialize() {
        androidEngine.initialize()
        geminiEngine.initialize()
        edgeEngine.initialize()
    }

    suspend fun getVoicesForProvider(providerId: String): List<VoiceDescriptor> {
        return when (providerId) {
            AppConstants.PROVIDER_GEMINI_TTS -> geminiEngine.getAvailableVoices("id")
            AppConstants.PROVIDER_EDGE_COMPATIBLE -> edgeEngine.getAvailableVoices("id")
            else -> androidEngine.getAvailableVoices("id")
        }
    }

    /**
     * Dual-Voice Engine synthesizer:
     * - Checks TtsAudioCache first when [enableCache] is true so identical dialogues are never regenerated.
     * - AUTOMATIC mode: uses preferred online AI engine when internet & credentials are available;
     *   falls back seamlessly to AndroidNativeVoiceEngine when offline or if the online call fails,
     *   and resumes AI synthesis automatically on the next dialogue once connectivity returns.
     * - MANUAL mode: respects [preferredProvider] directly; falls back to Android TTS only if an online
     *   provider errors so playback never crashes.
     * - Guarantees at most ONE SynthesizedAudio result per dialogue request (zero duplicate audio on switch).
     */
    suspend fun synthesize(
        preferredProvider: String,
        operatingMode: VoiceEngineOperatingMode,
        enableCache: Boolean,
        request: VoiceSynthesisRequest
    ): AppResult<SynthesizedAudio> {
        val isOnline = connectivityObserver.refresh()
        val chosenOnlineEngine = selectOnlineEngine(preferredProvider)

        val shouldAttemptOnline = when (operatingMode) {
            VoiceEngineOperatingMode.AUTOMATIC -> isOnline && chosenOnlineEngine != null && chosenOnlineEngine.isReady()
            VoiceEngineOperatingMode.MANUAL -> {
                preferredProvider != AppConstants.PROVIDER_ANDROID_TTS &&
                    isOnline &&
                    chosenOnlineEngine != null &&
                    chosenOnlineEngine.isReady()
            }
        }

        if (shouldAttemptOnline && chosenOnlineEngine != null) {
            val ext = if (chosenOnlineEngine.providerId == AppConstants.PROVIDER_EDGE_COMPATIBLE) "mp3" else "wav"
            val modelKey = if (chosenOnlineEngine.providerId == AppConstants.PROVIDER_GEMINI_TTS) {
                modelIdProvider()
            } else {
                "ws"
            }

            val cacheKey = audioCache.buildKey(
                providerId = chosenOnlineEngine.providerId,
                modelId = modelKey,
                voiceId = request.voiceId.ifBlank { request.personality.defaultGeminiVoice },
                personalityId = request.personality.id,
                emotionId = request.emotion.id,
                customDescription = request.customVoiceDescription,
                pitch = request.pitch,
                speechRate = request.speechRate,
                text = request.text
            )

            if (enableCache) {
                audioCache.get(cacheKey, ext)?.let { cachedFile ->
                    return AppResult.Success(
                        SynthesizedAudio(
                            audioFile = cachedFile,
                            providerUsed = chosenOnlineEngine.providerId,
                            usedFallback = false,
                            fromCache = true
                        )
                    )
                }
            }

            val onlineTemp = File(tempDir, "dub_${UUID.randomUUID()}.$ext")
            val onlineResult = chosenOnlineEngine.synthesizeToFile(request, onlineTemp)
            if (onlineResult is AppResult.Success) {
                val finalFile = if (enableCache) {
                    val cached = audioCache.put(cacheKey, ext, onlineResult.data)
                    if (onlineTemp.exists()) onlineTemp.delete()
                    cached
                } else {
                    onlineResult.data
                }
                return AppResult.Success(
                    SynthesizedAudio(
                        audioFile = finalFile,
                        providerUsed = chosenOnlineEngine.providerId,
                        usedFallback = false,
                        fromCache = false
                    )
                )
            } else {
                if (onlineTemp.exists()) onlineTemp.delete()
            }
        }

        // Offline Mode or Seamless Fallback -> Android Native TTS preserving character offline voice, pitch, and rate
        val offlineCacheKey = audioCache.buildKey(
            providerId = androidEngine.providerId,
            modelId = "native",
            voiceId = request.offlineVoiceId.ifBlank { request.voiceId },
            personalityId = request.personality.id,
            emotionId = request.emotion.id,
            customDescription = "",
            pitch = VoicePromptBuilder.computeOfflinePitch(request.pitch, request.emotion),
            speechRate = VoicePromptBuilder.computeOfflineRate(request.speechRate, request.emotion),
            text = request.text
        )

        if (enableCache) {
            audioCache.get(offlineCacheKey, "wav")?.let { cachedFile ->
                return AppResult.Success(
                    SynthesizedAudio(
                        audioFile = cachedFile,
                        providerUsed = androidEngine.providerId,
                        usedFallback = preferredProvider != AppConstants.PROVIDER_ANDROID_TTS,
                        fromCache = true
                    )
                )
            }
        }

        val wavFile = File(tempDir, "dub_${UUID.randomUUID()}.wav")
        val fallbackResult = androidEngine.synthesizeToFile(request, wavFile)
        return when (fallbackResult) {
            is AppResult.Success -> {
                val finalFile = if (enableCache) {
                    val cached = audioCache.put(offlineCacheKey, "wav", fallbackResult.data)
                    if (wavFile.exists()) wavFile.delete()
                    cached
                } else {
                    fallbackResult.data
                }
                AppResult.Success(
                    SynthesizedAudio(
                        audioFile = finalFile,
                        providerUsed = androidEngine.providerId,
                        usedFallback = preferredProvider != AppConstants.PROVIDER_ANDROID_TTS,
                        fromCache = false
                    )
                )
            }
            is AppResult.Error -> {
                if (wavFile.exists()) wavFile.delete()
                fallbackResult
            }
            AppResult.Loading -> AppResult.Loading
        }
    }

    private fun selectOnlineEngine(preferredProvider: String): VoiceEngine? {
        return when (preferredProvider) {
            AppConstants.PROVIDER_EDGE_COMPATIBLE -> edgeEngine
            AppConstants.PROVIDER_GEMINI_TTS -> geminiEngine
            AppConstants.PROVIDER_ANDROID_TTS -> null
            else -> geminiEngine
        }
    }

    fun getCacheSizeBytes(): Long = audioCache.totalSizeBytes()

    fun clearAudioCache() {
        audioCache.clear()
        cleanupStaleTempFiles()
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
        geminiEngine.release()
        edgeEngine.release()
        cleanupStaleTempFiles()
    }
}
