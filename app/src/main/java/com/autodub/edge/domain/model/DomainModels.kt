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

enum class VoiceProviderType(val id: String, val displayName: String) {
    ANDROID_TTS(AppConstants.PROVIDER_ANDROID_TTS, "Android Native TTS (Offline)"),
    EDGE_COMPATIBLE(AppConstants.PROVIDER_EDGE_COMPATIBLE, "Edge-Compatible WebSocket TTS")
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
    val voiceProvider: String = AppConstants.PROVIDER_ANDROID_TTS,
    val voiceId: String = "",
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
    val defaultProvider: VoiceProviderType = VoiceProviderType.ANDROID_TTS,
    val defaultVoiceId: String = "",
    val defaultPitch: Float = 1.0f,
    val defaultSpeechRate: Float = 1.0f,
    val defaultVolume: Float = 1.0f,
    val voiceEnabled: Boolean = true,
    val autoPlay: Boolean = true,
    val audioQueuePolicy: AudioQueuePolicy = AudioQueuePolicy.REPLACE_CURRENT,
    val sourceLanguageCode: String = AppConstants.DEFAULT_SOURCE_LANGUAGE,
    val targetLanguageCode: String = AppConstants.DEFAULT_TARGET_LANGUAGE,
    val ocrIntervalMs: Long = AppConstants.DEFAULT_FRAME_INTERVAL_MS,
    val translationDelayMs: Long = AppConstants.DEFAULT_TRANSLATION_DELAY_MS,
    val ocrSensitivity: Float = AppConstants.DEFAULT_OCR_SENSITIVITY,
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
    val timestampMs: Long = System.currentTimeMillis()
)

data class PipelineRuntimeState(
    val captureStatus: CaptureStatus = CaptureStatus.IDLE,
    val ocrStatus: OcrStatus = OcrStatus.IDLE,
    val translationModelState: TranslationModelState = TranslationModelState.Unknown,
    val voiceEngineStatus: VoiceEngineStatus = VoiceEngineStatus.INITIALIZING,
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
