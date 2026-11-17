package com.autodub.edge.domain.model

import com.autodub.edge.core.constants.AppConstants

enum class RoiMode {
    SETUP,
    SCAN
}

enum class AudioQueuePolicy(val label: String, val description: String) {
    REPLACE_CURRENT(
        label = "Replace Current",
        description = "Hentikan dialog lama dan langsung putar dialog terbaru."
    ),
    FINISH_CURRENT(
        label = "Finish Current",
        description = "Selesaikan dialog aktif lalu putar dialog terbaru."
    ),
    QUEUE_ALL(
        label = "Queue All",
        description = "Masukkan semua dialog baru ke dalam antrean berurutan."
    )
}

enum class VoiceEngineOperatingMode(val label: String, val description: String) {
    AUTOMATIC(
        label = "Automatic Mode (Dual Engine)",
        description = "Otomatis memakai AI Expressive TTS saat online, beralih ke Android TTS saat offline, dan kembali ke AI saat koneksi pulih."
    ),
    MANUAL(
        label = "Manual Mode",
        description = "Menggunakan provider suara pilihan pengguna secara langsung."
    )
}

enum class VoiceProviderType(val id: String, val displayName: String, val isOnlineProvider: Boolean) {
    GEMINI_TTS(AppConstants.PROVIDER_GEMINI_TTS, "Gemini Expressive AI TTS (Online)", true),
    EDGE_COMPATIBLE(AppConstants.PROVIDER_EDGE_COMPATIBLE, "Edge-Compatible WebSocket TTS (Online)", true),
    ANDROID_TTS(AppConstants.PROVIDER_ANDROID_TTS, "Android Native TTS (Offline)", false)
}

enum class VoicePersonality(
    val id: String,
    val displayName: String,
    val genderLabel: String,
    val defaultGeminiVoice: String,
    val stylePrompt: String
) {
    ANIME_FEMALE_GENTLE(
        id = "ANIME_FEMALE_GENTLE",
        displayName = "Sakura Gentle",
        genderLabel = "Perempuan · Lembut & Hangat",
        defaultGeminiVoice = "Kore",
        stylePrompt = "Suara perempuan dewasa bergaya anime Jepang yang lembut, hangat, feminin, ramah, dengan artikulasi bahasa Indonesia yang jelas."
    ),
    ANIME_FEMALE_CHEERFUL(
        id = "ANIME_FEMALE_CHEERFUL",
        displayName = "Hana Bright",
        genderLabel = "Perempuan · Ceria & Sedikit Manja",
        defaultGeminiVoice = "Zephyr",
        stylePrompt = "Suara perempuan dewasa bergaya anime Jepang yang lembut, ceria, sedikit manja, energik, dengan intonasi ekspresif dan artikulasi jelas."
    ),
    ANIME_FEMALE_COOL(
        id = "ANIME_FEMALE_COOL",
        displayName = "Rei Serene",
        genderLabel = "Perempuan · Kalem & Tenang",
        defaultGeminiVoice = "Aoede",
        stylePrompt = "Suara perempuan dewasa bergaya anime Jepang yang kalem, tenang, dewasa, penuh kendali, dan bernada halus."
    ),
    ANIME_FEMALE_EXPRESSIVE(
        id = "ANIME_FEMALE_EXPRESSIVE",
        displayName = "Aoi Dramatic",
        genderLabel = "Perempuan · Ekspresif & Dinamis",
        defaultGeminiVoice = "Leda",
        stylePrompt = "Suara perempuan dewasa bergaya anime Jepang yang sangat ekspresif, dinamis, penuh penjiwaan visual novel, dan responsif secara emosional."
    ),
    ANIME_MALE_CALM(
        id = "ANIME_MALE_CALM",
        displayName = "Kenji Calm",
        genderLabel = "Laki-laki · Kalem & Berwibawa",
        defaultGeminiVoice = "Charon",
        stylePrompt = "Suara laki-laki dewasa bergaya anime Jepang yang tenang, berwibawa, jernih, rendah, dan stabil."
    ),
    ANIME_MALE_ENERGETIC(
        id = "ANIME_MALE_ENERGETIC",
        displayName = "Ren Energetic",
        genderLabel = "Laki-laki · Energik & Tegas",
        defaultGeminiVoice = "Puck",
        stylePrompt = "Suara laki-laki dewasa bergaya anime Jepang yang energik, bersemangat, tegas, dan bertempo hidup."
    ),
    ANIME_MALE_MENTOR(
        id = "ANIME_MALE_MENTOR",
        displayName = "Haru Mentor",
        genderLabel = "Laki-laki · Hangat & Dewasa",
        defaultGeminiVoice = "Fenrir",
        stylePrompt = "Suara laki-laki dewasa yang hangat, dalam, bijaksana, cocok untuk karakter senior atau narator cerita."
    ),
    CUSTOM_ONLY(
        id = "CUSTOM_ONLY",
        displayName = "Custom Persona",
        genderLabel = "Kustom Bebas",
        defaultGeminiVoice = "Kore",
        stylePrompt = ""
    );

    companion object {
        fun fromId(id: String?): VoicePersonality {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
                ?: ANIME_FEMALE_GENTLE
        }
    }
}

enum class VoiceEmotion(
    val id: String,
    val displayName: String,
    val styleDirective: String,
    val offlinePitchDelta: Float,
    val offlineRateDelta: Float
) {
    NEUTRAL("NEUTRAL", "Natural", "Intonasi alami dan seimbang.", 0f, 0f),
    HAPPY("HAPPY", "Happy", "Nada gembira, cerah, dan tersenyum saat berbicara.", +0.06f, +0.05f),
    SAD("SAD", "Sad", "Nada sedih, lirih, lembut, dan sedikit melambat penuh haru.", -0.06f, -0.08f),
    ANGRY("ANGRY", "Angry", "Nada marah, tegas, intens, dan penekanan kuat.", +0.04f, +0.08f),
    EXCITED("EXCITED", "Excited", "Sangat bersemangat, antusias, ekspresif, dan bertempo cepat.", +0.10f, +0.10f),
    WHISPER("WHISPER", "Whisper", "Berbisik lembut, pelan, intim, dan penuh kehati-hatian.", -0.04f, -0.10f),
    CALM("CALM", "Calm", "Tenang, santai, lembut, dan menenangkan.", -0.02f, -0.04f),
    DRAMATIC("DRAMATIC", "Dramatic", "Dramatis, teatrikal, penuh penjiwaan dan jeda emosional.", 0f, -0.05f);

    companion object {
        fun fromId(id: String?): VoiceEmotion {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NEUTRAL
        }
    }
}

data class RoiBox(
    val leftFraction: Float,
    val topFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val borderColorArgb: Int,
    val fillAlpha: Float
) {
    fun clamped(): RoiBox {
        val w = widthFraction.coerceIn(0.08f, 0.96f)
        val h = heightFraction.coerceIn(0.05f, 0.65f)
        val l = leftFraction.coerceIn(0f, (1f - w).coerceAtLeast(0f))
        val t = topFraction.coerceIn(0f, (1f - h).coerceAtLeast(0f))
        return copy(
            leftFraction = l,
            topFraction = t,
            widthFraction = w,
            heightFraction = h,
            fillAlpha = fillAlpha.coerceIn(0.05f, 0.85f)
        )
    }

    companion object {
        val DEFAULT_NAME_ROI = RoiBox(
            leftFraction = AppConstants.DEFAULT_NAME_ROI_X,
            topFraction = AppConstants.DEFAULT_NAME_ROI_Y,
            widthFraction = AppConstants.DEFAULT_NAME_ROI_W,
            heightFraction = AppConstants.DEFAULT_NAME_ROI_H,
            borderColorArgb = AppConstants.DEFAULT_NAME_ROI_COLOR,
            fillAlpha = AppConstants.DEFAULT_ROI_ALPHA
        )

        val DEFAULT_DIALOG_ROI = RoiBox(
            leftFraction = AppConstants.DEFAULT_DIALOG_ROI_X,
            topFraction = AppConstants.DEFAULT_DIALOG_ROI_Y,
            widthFraction = AppConstants.DEFAULT_DIALOG_ROI_W,
            heightFraction = AppConstants.DEFAULT_DIALOG_ROI_H,
            borderColorArgb = AppConstants.DEFAULT_DIALOG_ROI_COLOR,
            fillAlpha = AppConstants.DEFAULT_ROI_ALPHA
        )
    }
}

data class RoiLayoutConfig(
    val nameRoi: RoiBox = RoiBox.DEFAULT_NAME_ROI,
    val dialogRoi: RoiBox = RoiBox.DEFAULT_DIALOG_ROI,
    val mode: RoiMode = RoiMode.SCAN
)

data class CharacterProfile(
    val id: Long = 0L,
    val characterName: String,
    val voiceProvider: String = AppConstants.PROVIDER_GEMINI_TTS,
    val voiceId: String = "Kore",
    val offlineVoiceId: String = "",
    val voicePersonality: VoicePersonality = VoicePersonality.ANIME_FEMALE_GENTLE,
    val emotion: VoiceEmotion = VoiceEmotion.NEUTRAL,
    val customVoiceDescription: String = "",
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val volume: Float = 1.0f,
    val enabled: Boolean = true,
    val customSsmlStyle: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class VoiceDescriptor(
    val id: String,
    val displayName: String,
    val localeTag: String,
    val providerId: String,
    val isNetworkRequired: Boolean = false
)

data class OverlayAppearanceConfig(
    val fontSizeSp: Float = AppConstants.DEFAULT_OVERLAY_FONT_SIZE_SP,
    val backgroundOpacity: Float = AppConstants.DEFAULT_OVERLAY_OPACITY,
    val widthFraction: Float = AppConstants.DEFAULT_OVERLAY_WIDTH_FRACTION,
    val textColorArgb: Int = AppConstants.DEFAULT_OVERLAY_TEXT_COLOR,
    val offsetXFraction: Float = 0.07f,
    val offsetYFraction: Float = 0.12f,
    val showOriginalText: Boolean = false,
    val isTranslationVisible: Boolean = true
)

data class DubbingSettings(
    val voiceOperatingMode: VoiceEngineOperatingMode = VoiceEngineOperatingMode.AUTOMATIC,
    val defaultProvider: VoiceProviderType = VoiceProviderType.GEMINI_TTS,
    val defaultVoiceId: String = "Kore",
    val defaultPersonality: VoicePersonality = VoicePersonality.ANIME_FEMALE_GENTLE,
    val defaultEmotion: VoiceEmotion = VoiceEmotion.NEUTRAL,
    val defaultCustomVoiceDescription: String = "",
    val defaultPitch: Float = 1.0f,
    val defaultSpeechRate: Float = 1.0f,
    val defaultVolume: Float = 1.0f,
    val voiceEnabled: Boolean = true,
    val autoPlay: Boolean = true,
    val enableAudioCache: Boolean = true,
    val audioQueuePolicy: AudioQueuePolicy = AudioQueuePolicy.REPLACE_CURRENT,
    val sourceLanguageCode: String = AppConstants.DEFAULT_SOURCE_LANGUAGE,
    val targetLanguageCode: String = AppConstants.DEFAULT_TARGET_LANGUAGE,
    val ocrIntervalMs: Long = AppConstants.DEFAULT_FRAME_INTERVAL_MS,
    val translationDelayMs: Long = AppConstants.DEFAULT_TRANSLATION_DELAY_MS,
    val ocrSensitivity: Float = AppConstants.DEFAULT_OCR_SENSITIVITY,
    val geminiModelId: String = AppConstants.DEFAULT_GEMINI_TTS_MODEL,
    val geminiEndpointUrl: String = "",
    val geminiApiKey: String = "",
    val edgeEndpointUrl: String = "",
    val edgeBearerToken: String = ""
)

sealed class TranslationModelState {
    data object Unknown : TranslationModelState()
    data object Checking : TranslationModelState()
    data class Downloading(val progressMessage: String) : TranslationModelState()
    data object Ready : TranslationModelState()
    data class Error(val message: String) : TranslationModelState()
}

enum class CaptureStatus {
    IDLE,
    REQUESTING_PERMISSION,
    RUNNING,
    PAUSED,
    ERROR
}

enum class OcrStatus {
    IDLE,
    SCANNING,
    PAUSED,
    ERROR
}

enum class VoiceEngineStatus {
    INITIALIZING,
    READY,
    SPEAKING,
    FALLBACK_ACTIVE,
    ERROR
}

data class DialogueEvent(
    val id: Long = System.currentTimeMillis(),
    val speakerName: String,
    val matchedProfileName: String,
    val sourceText: String,
    val translatedText: String,
    val voiceIdUsed: String,
    val providerUsed: String,
    val emotionUsed: String = VoiceEmotion.NEUTRAL.displayName,
    val cacheHit: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class PipelineRuntimeState(
    val captureStatus: CaptureStatus = CaptureStatus.IDLE,
    val ocrStatus: OcrStatus = OcrStatus.IDLE,
    val translationModelState: TranslationModelState = TranslationModelState.Unknown,
    val voiceEngineStatus: VoiceEngineStatus = VoiceEngineStatus.INITIALIZING,
    val voiceOperatingMode: VoiceEngineOperatingMode = VoiceEngineOperatingMode.AUTOMATIC,
    val isNetworkOnline: Boolean = true,
    val activeVoiceProviderLabel: String = "Siap",
    val lastCacheHit: Boolean = false,
    val isOverlayRunning: Boolean = false,
    val isTranslationPaused: Boolean = false,
    val isTranslationVisible: Boolean = true,
    val isVoiceEnabled: Boolean = true,
    val roiMode: RoiMode = RoiMode.SCAN,
    val currentSpeaker: String = AppConstants.DEFAULT_NARRATOR_DISPLAY,
    val currentSourceText: String = "",
    val currentTranslatedText: String = "",
    val lastErrorMessage: String? = null,
    val queueSize: Int = 0
)
