package com.autodub.edge.core.constants

object AppConstants {
    const val NOTIFICATION_CHANNEL_CAPTURE = "autodub_capture_channel"
    const val NOTIFICATION_CHANNEL_OVERLAY = "autodub_overlay_channel"
    const val NOTIFICATION_ID_CAPTURE = 1001
    const val NOTIFICATION_ID_OVERLAY = 1002

    const val DEFAULT_NARRATOR_NAME = "DEFAULT_NARRATOR"
    const val DEFAULT_NARRATOR_DISPLAY = "Default Narrator"

    const val PROVIDER_ANDROID_TTS = "ANDROID_TTS"
    const val PROVIDER_EDGE_COMPATIBLE = "EDGE_COMPATIBLE"
    const val PROVIDER_GEMINI_TTS = "GEMINI_TTS"

    const val DEFAULT_GEMINI_TTS_MODEL = "gemini-3.8-flash-tts"
    const val DEFAULT_GEMINI_LITE_TTS_MODEL = "gemini-3.8-flash-lite-tts"
    const val DEFAULT_GEMINI_BASE_URL = "https://generativelanguage.googleapis.com"

    const val TTS_CACHE_DIR = "tts_audio_cache"
    const val TTS_TEMP_DIR = "tts_temp"
    const val TTS_CACHE_MAX_BYTES = 40L * 1024L * 1024L
    const val GEMINI_HTTP_TIMEOUT_MS = 12_000L

    const val DEFAULT_SOURCE_LANGUAGE = "en"
    const val DEFAULT_TARGET_LANGUAGE = "id"

    const val DEFAULT_FRAME_INTERVAL_MS = 650L
    const val DEFAULT_TRANSLATION_DELAY_MS = 180L
    const val DEFAULT_OCR_SENSITIVITY = 0.78f

    // Normalized [0f..1f] default Dual-Box ROI layout for landscape/portrait VN dialog boxes
    const val DEFAULT_NAME_ROI_X = 0.08f
    const val DEFAULT_NAME_ROI_Y = 0.62f
    const val DEFAULT_NAME_ROI_W = 0.24f
    const val DEFAULT_NAME_ROI_H = 0.08f
    const val DEFAULT_NAME_ROI_COLOR = 0xFF00E5FF.toInt() // Cyan

    const val DEFAULT_DIALOG_ROI_X = 0.08f
    const val DEFAULT_DIALOG_ROI_Y = 0.71f
    const val DEFAULT_DIALOG_ROI_W = 0.84f
    const val DEFAULT_DIALOG_ROI_H = 0.22f
    const val DEFAULT_DIALOG_ROI_COLOR = 0xFFA855F7.toInt() // Cyber Purple

    const val DEFAULT_ROI_ALPHA = 0.35f

    const val DEFAULT_OVERLAY_FONT_SIZE_SP = 16f
    const val DEFAULT_OVERLAY_OPACITY = 0.82f
    const val DEFAULT_OVERLAY_WIDTH_FRACTION = 0.86f
    const val DEFAULT_OVERLAY_TEXT_COLOR = 0xFFFFFFFF.toInt()

    const val MAX_TRANSLATION_CACHE_SIZE = 250
    const val MAX_AUDIO_QUEUE_SIZE = 12
    const val EDGE_WS_TIMEOUT_MS = 8000L
}
