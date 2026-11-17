package com.autodub.edge.engine.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.util.AppError
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.domain.model.VoiceDescriptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AndroidNativeVoiceEngine(
    private val context: Context
) : VoiceEngine {

    override val providerId: String = AppConstants.PROVIDER_ANDROID_TTS
    override val supportsSsml: Boolean = false

    private val initMutex = Mutex()
    private val synthMutex = Mutex()

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var isInitialized: Boolean = false

    private val pendingUtterances = ConcurrentHashMap<String, CompletableDeferred<AppResult<File>>>()
    private val utteranceFiles = ConcurrentHashMap<String, File>()

    override suspend fun initialize(): AppResult<Unit> = initMutex.withLock {
        if (isInitialized && tts != null) {
            return AppResult.Success(Unit)
        }

        val deferred = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main) {
            tts?.shutdown()
            tts = TextToSpeech(context.applicationContext) { status ->
                deferred.complete(status == TextToSpeech.SUCCESS)
            }
        }

        val ok = withTimeoutOrNull(6000L) { deferred.await() } ?: false
        if (!ok) {
            isInitialized = false
            return AppResult.Error(
                AppError.TtsProviderFailed("Android TextToSpeech gagal diinisialisasi.")
            )
        }

        val engine = tts ?: return AppResult.Error(
            AppError.TtsProviderFailed("Instance TextToSpeech tidak tersedia.")
        )

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == null) return
                val file = utteranceFiles.remove(utteranceId)
                val waiter = pendingUtterances.remove(utteranceId)
                if (file != null && file.exists() && file.length() > 44L) {
                    waiter?.complete(AppResult.Success(file))
                } else {
                    waiter?.complete(
                        AppResult.Error(
                            AppError.TtsProviderFailed("Output audio TTS kosong.")
                        )
                    )
                }
            }

            @Deprecated("Deprecated in Android API, kept for API 26-29 compatibility")
            override fun onError(utteranceId: String?) {
                if (utteranceId == null) return
                utteranceFiles.remove(utteranceId)
                pendingUtterances.remove(utteranceId)?.complete(
                    AppResult.Error(AppError.TtsProviderFailed("Sintesis suara Android TTS gagal."))
                )
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId == null) return
                utteranceFiles.remove(utteranceId)
                pendingUtterances.remove(utteranceId)?.complete(
                    AppResult.Error(
                        AppError.TtsProviderFailed("Sintesis Android TTS gagal (kode: $errorCode).")
                    )
                )
            }
        })

        val indonesianLocale = Locale("id", "ID")
        val langResult = engine.setLanguage(indonesianLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fall back to default system locale if Indonesian pack is absent on emulator
            engine.setLanguage(Locale.getDefault())
        }

        isInitialized = true
        return AppResult.Success(Unit)
    }

    override suspend fun isReady(): Boolean = isInitialized && tts != null

    override suspend fun getAvailableVoices(languagePrefix: String): List<VoiceDescriptor> {
        if (!isInitialized) {
            initialize()
        }
        val engine = tts ?: return emptyList()
        val allVoices: Set<Voice> = runCatching { engine.voices }.getOrNull().orEmpty()
        val filtered = allVoices
            .filter { voice ->
                voice.locale.language.equals(languagePrefix, ignoreCase = true) ||
                    voice.locale.language.equals("en", ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<Voice> { it.locale.language.equals(languagePrefix, ignoreCase = true) }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenBy { it.name }
            )

        return filtered.map { voice ->
            VoiceDescriptor(
                id = voice.name,
                displayName = "${voice.locale.displayLanguage} (${voice.name.takeLast(14)})",
                localeTag = voice.locale.toLanguageTag(),
                providerId = providerId,
                isNetworkRequired = voice.isNetworkConnectionRequired
            )
        }
    }

    override suspend fun synthesizeToFile(
        request: VoiceSynthesisRequest,
        outputFile: File
    ): AppResult<File> = synthMutex.withLock {
        if (!isInitialized || tts == null) {
            val initRes = initialize()
            if (initRes is AppResult.Error) return@withLock initRes
        }

        val engine = tts ?: return@withLock AppResult.Error(
            AppError.TtsProviderFailed("Android TTS belum siap.")
        )

        val plainParams = SSMLGenerator.toPlainSpeechParams(
            request.text,
            SsmlConfig(
                voiceId = request.voiceId,
                pitchMultiplier = request.pitch,
                rateMultiplier = request.speechRate,
                languageTag = request.languageTag
            )
        )
        if (plainParams.text.isBlank()) {
            return@withLock AppResult.Error(AppError.TtsProviderFailed("Teks dialog kosong."))
        }

        outputFile.parentFile?.mkdirs()

        // Apply voice if available on device
        if (plainParams.voiceId.isNotEmpty()) {
            val matchedVoice = runCatching { engine.voices }
                .getOrNull()
                ?.firstOrNull { it.name == plainParams.voiceId }
            if (matchedVoice != null) {
                engine.voice = matchedVoice
            }
        } else {
            val idLocale = Locale("id", "ID")
            val avail = engine.isLanguageAvailable(idLocale)
            if (avail >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = idLocale
            }
        }

        engine.setPitch(plainParams.pitch)
        engine.setSpeechRate(plainParams.rate)

        val utteranceId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<AppResult<File>>()
        pendingUtterances[utteranceId] = deferred
        utteranceFiles[utteranceId] = outputFile

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, request.volume.coerceIn(0f, 1f))
        }

        val queueStatus = engine.synthesizeToFile(plainParams.text, params, outputFile, utteranceId)
        if (queueStatus != TextToSpeech.SUCCESS) {
            pendingUtterances.remove(utteranceId)
            utteranceFiles.remove(utteranceId)
            return@withLock AppResult.Error(
                AppError.TtsProviderFailed("Gagal menjadwalkan sintesis file Android TTS.")
            )
        }

        return@withLock withTimeoutOrNull(12_000L) {
            deferred.await()
        } ?: run {
            pendingUtterances.remove(utteranceId)
            utteranceFiles.remove(utteranceId)
            AppResult.Error(AppError.TtsProviderFailed("Sintesis Android TTS melebihi batas waktu."))
        }
    }

    override fun release() {
        pendingUtterances.values.forEach { it.cancel() }
        pendingUtterances.clear()
        utteranceFiles.clear()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
