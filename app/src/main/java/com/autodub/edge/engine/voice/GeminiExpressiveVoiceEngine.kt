package com.autodub.edge.engine.voice

import android.util.Base64
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.util.AppError
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.domain.model.VoiceDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Official Gemini Generative Language TTS engine supporting:
 * - Voice Design via `speechMetadata.style` (Anime-inspired persona, emotion, custom voice description)
 * - Model selection (`gemini-3.8-flash-tts` for expressive Voice Design, `gemini-3.8-flash-lite-tts` for fast dubbing)
 * - Runtime user-configured API key or authenticated proxy URL (ZERO hardcoded secrets in APK)
 */
class GeminiExpressiveVoiceEngine(
    private val modelProvider: () -> String,
    private val endpointOverrideProvider: () -> String,
    private val apiKeyProvider: () -> String
) : VoiceEngine {

    override val providerId: String = AppConstants.PROVIDER_GEMINI_TTS
    override val supportsSsml: Boolean = false
    override val supportsExpressiveDesign: Boolean = true

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppConstants.GEMINI_HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(AppConstants.GEMINI_HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(AppConstants.GEMINI_HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun initialize(): AppResult<Unit> {
        return if (isReady()) {
            AppResult.Success(Unit)
        } else {
            AppResult.Error(
                AppError.TtsProviderFailed(
                    "Gemini TTS membutuhkan API key runtime atau endpoint proxy di Pengaturan Voice."
                )
            )
        }
    }

    override suspend fun isReady(): Boolean {
        val customEndpoint = endpointOverrideProvider().trim()
        val key = apiKeyProvider().trim()
        return customEndpoint.startsWith("https://") ||
            customEndpoint.startsWith("http://") ||
            key.isNotEmpty()
    }

    override suspend fun getAvailableVoices(languagePrefix: String): List<VoiceDescriptor> {
        return listOf(
            VoiceDescriptor("Kore", "Kore · Perempuan Lembut / Hangat", "multi", providerId, true),
            VoiceDescriptor("Zephyr", "Zephyr · Perempuan Ceria / Energik", "multi", providerId, true),
            VoiceDescriptor("Aoede", "Aoede · Perempuan Kalem / Elegan", "multi", providerId, true),
            VoiceDescriptor("Leda", "Leda · Perempuan Ekspresif / Dramatis", "multi", providerId, true),
            VoiceDescriptor("Charon", "Charon · Laki-laki Kalem / Berwibawa", "multi", providerId, true),
            VoiceDescriptor("Puck", "Puck · Laki-laki Energik / Dinamis", "multi", providerId, true),
            VoiceDescriptor("Fenrir", "Fenrir · Laki-laki Dalam / Mentor", "multi", providerId, true),
            VoiceDescriptor("Orus", "Orus · Laki-laki Tegas / Narator", "multi", providerId, true)
        )
    }

    override suspend fun synthesizeToFile(
        request: VoiceSynthesisRequest,
        outputFile: File
    ): AppResult<File> = withContext(Dispatchers.IO) {
        val cleanText = request.text.trim()
        if (cleanText.isEmpty()) {
            return@withContext AppResult.Error(AppError.TtsProviderFailed("Teks dialog kosong."))
        }

        val customEndpoint = endpointOverrideProvider().trim()
        val apiKey = apiKeyProvider().trim()
        val model = modelProvider().trim().ifBlank { AppConstants.DEFAULT_GEMINI_TTS_MODEL }

        if (customEndpoint.isEmpty() && apiKey.isEmpty()) {
            return@withContext AppResult.Error(
                AppError.TtsProviderFailed("API key Gemini belum diisi di Pengaturan Voice.")
            )
        }

        val targetUrl = if (customEndpoint.isNotEmpty()) {
            customEndpoint
        } else {
            "${AppConstants.DEFAULT_GEMINI_BASE_URL}/v1beta/models/$model:generateContent"
        }

        val selectedVoice = request.voiceId.trim().ifBlank {
            request.personality.defaultGeminiVoice
        }

        val stylePrompt = VoicePromptBuilder.buildStylePrompt(
            personality = request.personality,
            emotion = request.emotion,
            customDescription = request.customVoiceDescription,
            pitch = request.pitch,
            speechRate = request.speechRate
        )

        val requestJson = buildGeminiTtsPayload(
            text = cleanText,
            stylePrompt = stylePrompt,
            voiceName = selectedVoice
        )

        val httpReqBuilder = Request.Builder()
            .url(targetUrl)
            .post(requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addHeader("Content-Type", "application/json")

        if (apiKey.isNotEmpty()) {
            httpReqBuilder.addHeader("x-goog-api-key", apiKey)
        }

        return@withContext try {
            client.newCall(httpReqBuilder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        400, 401, 403 -> "API key Gemini tidak valid atau akses ditolak (${response.code})."
                        404 -> "Model Gemini TTS '$model' tidak ditemukan (404)."
                        429 -> "Kuota Gemini TTS tercapai (429). Beralih ke fallback offline."
                        else -> "Gemini TTS error HTTP ${response.code}."
                    }
                    return@withContext AppResult.Error(AppError.TtsProviderFailed(errorMsg))
                }

                val wavBytes = parseWavBytesFromResponse(rawBody)
                if (wavBytes == null || wavBytes.size <= 44) {
                    return@withContext AppResult.Error(
                        AppError.TtsProviderFailed("Respons Gemini TTS tidak berisi data audio WAV yang valid.")
                    )
                }

                outputFile.parentFile?.mkdirs()
                outputFile.writeBytes(wavBytes)
                AppResult.Success(outputFile)
            }
        } catch (t: Throwable) {
            AppResult.Error(
                AppError.NetworkUnavailable(t.message ?: "Gagal menghubungi server Gemini TTS.", t)
            )
        }
    }

    internal fun buildGeminiTtsPayload(
        text: String,
        stylePrompt: String,
        voiceName: String
    ): JSONObject {
        val speechMeta = JSONObject().put("style", stylePrompt)
        val part = JSONObject()
            .put("text", text)
            .put("speechMetadata", speechMeta)

        val content = JSONObject()
            .put("role", "user")
            .put("parts", JSONArray().put(part))

        val prebuiltVoice = JSONObject().put("voiceName", voiceName)
        val voiceConfig = JSONObject().put("prebuiltVoiceConfig", prebuiltVoice)
        val speechConfig = JSONObject().put("voiceConfig", voiceConfig)
        val generationConfig = JSONObject()
            .put("responseModalities", JSONArray().put("AUDIO"))
            .put("speechConfig", speechConfig)

        return JSONObject()
            .put("contents", JSONArray().put(content))
            .put("generationConfig", generationConfig)
    }

    internal fun parseWavBytesFromResponse(rawJson: String): ByteArray? {
        return runCatching {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val parts = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts") ?: return null

            for (i in 0 until parts.length()) {
                val inlineData = parts.getJSONObject(i).optJSONObject("inlineData") ?: continue
                val b64 = inlineData.optString("data", "")
                if (b64.isNotBlank()) {
                    return Base64.decode(b64, Base64.DEFAULT)
                }
            }
            null
        }.getOrNull()
    }

    override fun release() {
        client.dispatcher.executorService.shutdown()
    }
}
