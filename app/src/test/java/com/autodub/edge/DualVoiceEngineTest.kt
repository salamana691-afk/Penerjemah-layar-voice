package com.autodub.edge

import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.network.ConnectivityObserver
import com.autodub.edge.core.util.AppError
import com.autodub.edge.core.util.AppResult
import com.autodub.edge.domain.model.VoiceDescriptor
import com.autodub.edge.domain.model.VoiceEmotion
import com.autodub.edge.domain.model.VoiceEngineOperatingMode
import com.autodub.edge.domain.model.VoicePersonality
import com.autodub.edge.engine.audio.TtsAudioCache
import com.autodub.edge.engine.voice.VoiceEngine
import com.autodub.edge.engine.voice.VoiceOrchestrator
import com.autodub.edge.engine.voice.VoicePromptBuilder
import com.autodub.edge.engine.voice.VoiceSynthesisRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private class FakeConnectivityObserver(initialOnline: Boolean) : ConnectivityObserver {
    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline
    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
    override fun refresh(): Boolean = _isOnline.value
    override fun release() {}
}

private class FakeVoiceEngine(
    override val providerId: String,
    private val ready: Boolean = true,
    var shouldFail: Boolean = false
) : VoiceEngine {
    override val supportsSsml: Boolean = false
    var synthesizeCalls: Int = 0
    var lastRequest: VoiceSynthesisRequest? = null

    override suspend fun initialize(): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun isReady(): Boolean = ready
    override suspend fun getAvailableVoices(languagePrefix: String): List<VoiceDescriptor> = emptyList()

    override suspend fun synthesizeToFile(
        request: VoiceSynthesisRequest,
        outputFile: File
    ): AppResult<File> {
        synthesizeCalls++
        lastRequest = request
        if (shouldFail) {
            return AppResult.Error(AppError.TtsProviderFailed("Simulated $providerId error"))
        }
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(ByteArray(128) { 1 })
        return AppResult.Success(outputFile)
    }

    override fun release() {}
}

class DualVoiceEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `voice prompt builder combines anime personality emotion and custom description`() {
        val prompt = VoicePromptBuilder.buildStylePrompt(
            personality = VoicePersonality.ANIME_FEMALE_CHEERFUL,
            emotion = VoiceEmotion.EXCITED,
            customDescription = "Suara perempuan dewasa bergaya anime Jepang, lembut, ceria, sedikit manja.",
            pitch = 1.18f,
            speechRate = 1.15f
        )
        assertTrue(prompt.contains("sedikit manja"))
        assertTrue(prompt.contains("Excited"))
        assertTrue(prompt.contains("Hana Bright"))
        assertTrue(prompt.contains("lebih tinggi"))
    }

    @Test
    fun `offline pitch and rate incorporate emotion deltas safely within bounds`() {
        val sadPitch = VoicePromptBuilder.computeOfflinePitch(1.0f, VoiceEmotion.SAD)
        val excitedRate = VoicePromptBuilder.computeOfflineRate(1.95f, VoiceEmotion.EXCITED)
        assertTrue(sadPitch < 1.0f)
        assertEquals(2.0f, excitedRate, 0.001f)
    }

    @Test
    fun `automatic mode switches online to offline fallback and recovers when online again without duplicate synthesis`() = runTest {
        val connectivity = FakeConnectivityObserver(initialOnline = true)
        val androidEngine = FakeVoiceEngine(AppConstants.PROVIDER_ANDROID_TTS)
        val geminiEngine = FakeVoiceEngine(AppConstants.PROVIDER_GEMINI_TTS)
        val edgeEngine = FakeVoiceEngine(AppConstants.PROVIDER_EDGE_COMPATIBLE, ready = false)
        val cache = TtsAudioCache(tempFolder.newFolder("audio_cache"))

        val orchestrator = VoiceOrchestrator(
            context = android.app.Application(),
            androidEngine = androidEngine,
            geminiEngine = geminiEngine,
            edgeEngine = edgeEngine,
            connectivityObserver = connectivity,
            audioCache = cache,
            tempDirOverride = tempFolder.newFolder("tts_temp_${System.nanoTime()}")
        )

        val req1 = VoiceSynthesisRequest(
            text = "Selamat pagi, apakah kamu sudah siap?",
            voiceId = "Zephyr",
            offlineVoiceId = "id-id-x-idd-local",
            personality = VoicePersonality.ANIME_FEMALE_CHEERFUL,
            emotion = VoiceEmotion.HAPPY,
            customVoiceDescription = "Suara ceria",
            pitch = 1.08f,
            speechRate = 1.04f,
            volume = 1.0f
        )

        // 1. Online -> uses Gemini TTS
        val res1 = orchestrator.synthesize(
            preferredProvider = AppConstants.PROVIDER_GEMINI_TTS,
            operatingMode = VoiceEngineOperatingMode.AUTOMATIC,
            enableCache = true,
            request = req1
        )
        assertTrue(res1 is AppResult.Success)
        assertEquals(AppConstants.PROVIDER_GEMINI_TTS, (res1 as AppResult.Success).data.providerUsed)
        assertFalse(res1.data.usedFallback)
        assertFalse(res1.data.fromCache)
        assertEquals(1, geminiEngine.synthesizeCalls)
        assertEquals(0, androidEngine.synthesizeCalls)

        // 2. Identical dialogue & config -> hits disk cache without regenerating
        val res1Cached = orchestrator.synthesize(
            preferredProvider = AppConstants.PROVIDER_GEMINI_TTS,
            operatingMode = VoiceEngineOperatingMode.AUTOMATIC,
            enableCache = true,
            request = req1
        )
        assertTrue((res1Cached as AppResult.Success).data.fromCache)
        assertEquals(1, geminiEngine.synthesizeCalls)

        // 3. Internet drops -> Automatic fallback to Android Native TTS preserving offlineVoiceId
        connectivity.setOnline(false)
        val req2 = req1.copy(text = "Awan gelap mulai berkumpul di langit.")
        val res2 = orchestrator.synthesize(
            preferredProvider = AppConstants.PROVIDER_GEMINI_TTS,
            operatingMode = VoiceEngineOperatingMode.AUTOMATIC,
            enableCache = true,
            request = req2
        )
        assertTrue(res2 is AppResult.Success)
        assertEquals(AppConstants.PROVIDER_ANDROID_TTS, (res2 as AppResult.Success).data.providerUsed)
        assertTrue(res2.data.usedFallback)
        assertEquals("id-id-x-idd-local", androidEngine.lastRequest?.offlineVoiceId)
        assertEquals(1, geminiEngine.synthesizeCalls)
        assertEquals(1, androidEngine.synthesizeCalls)

        // 4. Internet recovers -> next new dialogue resumes Gemini TTS
        connectivity.setOnline(true)
        val req3 = req1.copy(text = "Koneksi kita kembali terhubung!")
        val res3 = orchestrator.synthesize(
            preferredProvider = AppConstants.PROVIDER_GEMINI_TTS,
            operatingMode = VoiceEngineOperatingMode.AUTOMATIC,
            enableCache = true,
            request = req3
        )
        assertTrue(res3 is AppResult.Success)
        assertEquals(AppConstants.PROVIDER_GEMINI_TTS, (res3 as AppResult.Success).data.providerUsed)
        assertFalse(res3.data.usedFallback)
        assertEquals(2, geminiEngine.synthesizeCalls)
        assertEquals(1, androidEngine.synthesizeCalls)
    }

    @Test
    fun `manual android mode stays offline even when internet is available`() = runTest {
        val connectivity = FakeConnectivityObserver(initialOnline = true)
        val androidEngine = FakeVoiceEngine(AppConstants.PROVIDER_ANDROID_TTS)
        val geminiEngine = FakeVoiceEngine(AppConstants.PROVIDER_GEMINI_TTS)
        val edgeEngine = FakeVoiceEngine(AppConstants.PROVIDER_EDGE_COMPATIBLE, ready = false)
        val cache = TtsAudioCache(tempFolder.newFolder("manual_cache"))

        val orchestrator = VoiceOrchestrator(
            context = android.app.Application(),
            androidEngine = androidEngine,
            geminiEngine = geminiEngine,
            edgeEngine = edgeEngine,
            connectivityObserver = connectivity,
            audioCache = cache,
            tempDirOverride = tempFolder.newFolder("tts_temp_${System.nanoTime()}")
        )

        val req = VoiceSynthesisRequest(
            text = "Mode manual offline aktif.",
            voiceId = "id-id-x-idd-local",
            pitch = 1.0f,
            speechRate = 1.0f,
            volume = 1.0f
        )

        val res = orchestrator.synthesize(
            preferredProvider = AppConstants.PROVIDER_ANDROID_TTS,
            operatingMode = VoiceEngineOperatingMode.MANUAL,
            enableCache = false,
            request = req
        )

        assertTrue(res is AppResult.Success)
        assertEquals(AppConstants.PROVIDER_ANDROID_TTS, (res as AppResult.Success).data.providerUsed)
        assertEquals(0, geminiEngine.synthesizeCalls)
        assertEquals(1, androidEngine.synthesizeCalls)
    }
}
