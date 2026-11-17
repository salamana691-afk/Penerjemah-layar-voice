package com.autodub.edge.engine.voice

import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.util.AppError
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.domain.model.VoiceDescriptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Modular OkHttp WebSocket TTS client for user-configured, authorized Edge/Azure-compatible
 * SSML Read-Aloud WebSocket endpoints.
 *
 * Security & compliance:
 * - No hardcoded API keys or secret tokens.
 * - Requires explicit user-provided WebSocket URL in settings.
 * - Falls back automatically via VoiceOrchestrator to AndroidNativeVoiceEngine when unconfigured or offline.
 */
class EdgeCompatibleVoiceEngine(
    private val endpointProvider: () -> String,
    private val tokenProvider: () -> String
) : VoiceEngine {

    override val providerId: String = AppConstants.PROVIDER_EDGE_COMPATIBLE
    override val supportsSsml: Boolean = true

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppConstants.EDGE_WS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(AppConstants.EDGE_WS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(AppConstants.EDGE_WS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun initialize(): AppResult<Unit> {
        val url = endpointProvider().trim()
        return if (url.startsWith("wss://") || url.startsWith("ws://")) {
            AppResult.Success(Unit)
        } else {
            AppResult.Error(
                AppError.TtsProviderFailed(
                    "Endpoint WebSocket TTS kompatibel belum dikonfigurasi."
                )
            )
        }
    }

    override suspend fun isReady(): Boolean {
        val url = endpointProvider().trim()
        return url.startsWith("wss://") || url.startsWith("ws://")
    }

    override suspend fun getAvailableVoices(languagePrefix: String): List<VoiceDescriptor> {
        // Catalog of standard neural voice IDs for authorized Edge-compatible SSML endpoints
        return listOf(
            VoiceDescriptor(
                id = "id-ID-GadisNeural",
                displayName = "Gadis Neural (id-ID)",
                localeTag = "id-ID",
                providerId = providerId,
                isNetworkRequired = true
            ),
            VoiceDescriptor(
                id = "id-ID-ArdiNeural",
                displayName = "Ardi Neural (id-ID)",
                localeTag = "id-ID",
                providerId = providerId,
                isNetworkRequired = true
            ),
            VoiceDescriptor(
                id = "en-US-JennyNeural",
                displayName = "Jenny Neural (en-US)",
                localeTag = "en-US",
                providerId = providerId,
                isNetworkRequired = true
            ),
            VoiceDescriptor(
                id = "en-US-GuyNeural",
                displayName = "Guy Neural (en-US)",
                localeTag = "en-US",
                providerId = providerId,
                isNetworkRequired = true
            )
        )
    }

    override suspend fun synthesizeToFile(
        request: VoiceSynthesisRequest,
        outputFile: File
    ): AppResult<File> = withContext(Dispatchers.IO) {
        val wsUrl = endpointProvider().trim()
        if (!wsUrl.startsWith("wss://") && !wsUrl.startsWith("ws://")) {
            return@withContext AppResult.Error(
                AppError.TtsProviderFailed("Endpoint WebSocket TTS belum dikonfigurasi.")
            )
        }

        val ssml = SSMLGenerator.generate(
            text = request.text,
            config = SsmlConfig(
                voiceId = request.voiceId.ifBlank { "id-ID-GadisNeural" },
                pitchMultiplier = request.pitch,
                rateMultiplier = request.speechRate,
                languageTag = request.languageTag,
                style = request.customSsmlStyle
            )
        )

        val requestId = UUID.randomUUID().toString().replace("-", "")
        val reqBuilder = Request.Builder().url(wsUrl)
        val token = tokenProvider().trim()
        if (token.isNotEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        val resultDeferred = CompletableDeferred<AppResult<ByteArray>>()
        val audioBuffer = ByteArrayOutputStream()

        val ws = client.newWebSocket(reqBuilder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val configMessage = buildString {
                    append("Content-Type:application/json; charset=utf-8\r\n")
                    append("Path:speech.config\r\n\r\n")
                    append("""{"context":{"synthesis":{"audio":{"metadataoptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-48kbitrate-mono-mp3"}}}}""")
                }
                webSocket.send(configMessage)

                val ssmlMessage = buildString {
                    append("X-RequestId:$requestId\r\n")
                    append("Content-Type:application/ssml+xml\r\n")
                    append("Path:ssml\r\n\r\n")
                    append(ssml)
                }
                webSocket.send(ssmlMessage)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("Path:turn.end")) {
                    val bytes = audioBuffer.toByteArray()
                    if (bytes.isNotEmpty()) {
                        resultDeferred.complete(AppResult.Success(bytes))
                    } else {
                        resultDeferred.complete(
                            AppResult.Error(
                                AppError.TtsProviderFailed("Server TTS mengembalikan audio kosong.")
                            )
                        )
                    }
                    webSocket.close(1000, "Done")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val raw = bytes.toByteArray()
                val audioPayload = extractBinaryAudioPayload(raw)
                if (audioPayload.isNotEmpty()) {
                    audioBuffer.write(audioPayload)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!resultDeferred.isCompleted) {
                    val detail = response?.message ?: t.message ?: "Koneksi WebSocket TTS gagal."
                    resultDeferred.complete(
                        AppResult.Error(AppError.TtsProviderFailed(detail, t))
                    )
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!resultDeferred.isCompleted) {
                    val bytes = audioBuffer.toByteArray()
                    if (bytes.isNotEmpty()) {
                        resultDeferred.complete(AppResult.Success(bytes))
                    } else {
                        resultDeferred.complete(
                            AppResult.Error(
                                AppError.TtsProviderFailed("Koneksi ditutup ($code): $reason")
                            )
                        )
                    }
                }
            }
        })

        val outcome = withTimeoutOrNull(AppConstants.EDGE_WS_TIMEOUT_MS) {
            resultDeferred.await()
        } ?: run {
            ws.cancel()
            AppResult.Error(AppError.TtsProviderFailed("Timeout koneksi WebSocket TTS."))
        }

        when (outcome) {
            is AppResult.Success -> {
                try {
                    outputFile.parentFile?.mkdirs()
                    outputFile.writeBytes(outcome.data)
                    AppResult.Success(outputFile)
                } catch (t: Throwable) {
                    AppResult.Error(AppError.AudioPlaybackFailed("Gagal menyimpan file audio.", t))
                }
            }
            is AppResult.Error -> outcome
            AppResult.Loading -> AppResult.Error(AppError.TtsProviderFailed("Status tidak valid."))
        }
    }

    /**
     * Parses binary speech protocol frames: 2-byte big-endian header length followed by header text,
     * followed by encoded audio payload. If frame does not match header format, returns empty array.
     */
    private fun extractBinaryAudioPayload(frame: ByteArray): ByteArray {
        if (frame.size < 4) return ByteArray(0)
        val headerLen = ((frame[0].toInt() and 0xFF) shl 8) or (frame[1].toInt() and 0xFF)
        val payloadOffset = 2 + headerLen
        if (headerLen <= 0 || payloadOffset >= frame.size) return ByteArray(0)
        val headerText = String(frame, 2, headerLen, Charsets.UTF_8)
        if (!headerText.contains("Path:audio", ignoreCase = true)) return ByteArray(0)
        return frame.copyOfRange(payloadOffset, frame.size)
    }

    override fun release() {
        client.dispatcher.executorService.shutdown()
    }
}
