package com.autodub.edge.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.AudioQueuePolicy
import com.autodub.edge.domain.model.DubbingSettings
import com.autodub.edge.domain.model.OverlayAppearanceConfig
import com.autodub.edge.domain.model.RoiBox
import com.autodub.edge.domain.model.RoiLayoutConfig
import com.autodub.edge.domain.model.RoiMode
import com.autodub.edge.domain.model.VoiceProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.autoDubDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "autodub_edge_preferences"
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val NAME_ROI_X = floatPreferencesKey("name_roi_x")
        val NAME_ROI_Y = floatPreferencesKey("name_roi_y")
        val NAME_ROI_W = floatPreferencesKey("name_roi_w")
        val NAME_ROI_H = floatPreferencesKey("name_roi_h")
        val NAME_ROI_COLOR = intPreferencesKey("name_roi_color")
        val NAME_ROI_ALPHA = floatPreferencesKey("name_roi_alpha")

        val DIALOG_ROI_X = floatPreferencesKey("dialog_roi_x")
        val DIALOG_ROI_Y = floatPreferencesKey("dialog_roi_y")
        val DIALOG_ROI_W = floatPreferencesKey("dialog_roi_w")
        val DIALOG_ROI_H = floatPreferencesKey("dialog_roi_h")
        val DIALOG_ROI_COLOR = intPreferencesKey("dialog_roi_color")
        val DIALOG_ROI_ALPHA = floatPreferencesKey("dialog_roi_alpha")

        val ROI_MODE = stringPreferencesKey("roi_mode")

        val OVERLAY_FONT_SIZE = floatPreferencesKey("overlay_font_size")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val OVERLAY_WIDTH = floatPreferencesKey("overlay_width")
        val OVERLAY_TEXT_COLOR = intPreferencesKey("overlay_text_color")
        val OVERLAY_OFFSET_X = floatPreferencesKey("overlay_offset_x")
        val OVERLAY_OFFSET_Y = floatPreferencesKey("overlay_offset_y")
        val OVERLAY_SHOW_ORIGINAL = booleanPreferencesKey("overlay_show_original")
        val OVERLAY_VISIBLE = booleanPreferencesKey("overlay_visible")

        val VOICE_PROVIDER = stringPreferencesKey("voice_provider")
        val VOICE_ID = stringPreferencesKey("voice_id")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val VOICE_RATE = floatPreferencesKey("voice_rate")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val QUEUE_POLICY = stringPreferencesKey("queue_policy")
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val OCR_INTERVAL_MS = longPreferencesKey("ocr_interval_ms")
        val TRANSLATION_DELAY_MS = longPreferencesKey("translation_delay_ms")
        val OCR_SENSITIVITY = floatPreferencesKey("ocr_sensitivity")
        val EDGE_ENDPOINT_URL = stringPreferencesKey("edge_endpoint_url")
        val EDGE_BEARER_TOKEN = stringPreferencesKey("edge_bearer_token")
    }

    private val safeData: Flow<Preferences> = context.autoDubDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val roiLayoutFlow: Flow<RoiLayoutConfig> = safeData.map { prefs ->
        val nameBox = RoiBox(
            leftFraction = prefs[Keys.NAME_ROI_X] ?: AppConstants.DEFAULT_NAME_ROI_X,
            topFraction = prefs[Keys.NAME_ROI_Y] ?: AppConstants.DEFAULT_NAME_ROI_Y,
            widthFraction = prefs[Keys.NAME_ROI_W] ?: AppConstants.DEFAULT_NAME_ROI_W,
            heightFraction = prefs[Keys.NAME_ROI_H] ?: AppConstants.DEFAULT_NAME_ROI_H,
            borderColorArgb = prefs[Keys.NAME_ROI_COLOR] ?: AppConstants.DEFAULT_NAME_ROI_COLOR,
            fillAlpha = prefs[Keys.NAME_ROI_ALPHA] ?: AppConstants.DEFAULT_ROI_ALPHA
        ).clamped()

        val dialogBox = RoiBox(
            leftFraction = prefs[Keys.DIALOG_ROI_X] ?: AppConstants.DEFAULT_DIALOG_ROI_X,
            topFraction = prefs[Keys.DIALOG_ROI_Y] ?: AppConstants.DEFAULT_DIALOG_ROI_Y,
            widthFraction = prefs[Keys.DIALOG_ROI_W] ?: AppConstants.DEFAULT_DIALOG_ROI_W,
            heightFraction = prefs[Keys.DIALOG_ROI_H] ?: AppConstants.DEFAULT_DIALOG_ROI_H,
            borderColorArgb = prefs[Keys.DIALOG_ROI_COLOR] ?: AppConstants.DEFAULT_DIALOG_ROI_COLOR,
            fillAlpha = prefs[Keys.DIALOG_ROI_ALPHA] ?: AppConstants.DEFAULT_ROI_ALPHA
        ).clamped()

        val mode = runCatching {
            RoiMode.valueOf(prefs[Keys.ROI_MODE] ?: RoiMode.SCAN.name)
        }.getOrDefault(RoiMode.SCAN)

        RoiLayoutConfig(nameRoi = nameBox, dialogRoi = dialogBox, mode = mode)
    }

    val overlayConfigFlow: Flow<OverlayAppearanceConfig> = safeData.map { prefs ->
        OverlayAppearanceConfig(
            fontSizeSp = (prefs[Keys.OVERLAY_FONT_SIZE] ?: AppConstants.DEFAULT_OVERLAY_FONT_SIZE_SP).coerceIn(11f, 30f),
            backgroundOpacity = (prefs[Keys.OVERLAY_OPACITY] ?: AppConstants.DEFAULT_OVERLAY_OPACITY).coerceIn(0.1f, 1.0f),
            widthFraction = (prefs[Keys.OVERLAY_WIDTH] ?: AppConstants.DEFAULT_OVERLAY_WIDTH_FRACTION).coerceIn(0.4f, 0.98f),
            textColorArgb = prefs[Keys.OVERLAY_TEXT_COLOR] ?: AppConstants.DEFAULT_OVERLAY_TEXT_COLOR,
            offsetXFraction = (prefs[Keys.OVERLAY_OFFSET_X] ?: 0.07f).coerceIn(0f, 0.8f),
            offsetYFraction = (prefs[Keys.OVERLAY_OFFSET_Y] ?: 0.12f).coerceIn(0f, 0.85f),
            showOriginalText = prefs[Keys.OVERLAY_SHOW_ORIGINAL] ?: false,
            isTranslationVisible = prefs[Keys.OVERLAY_VISIBLE] ?: true
        )
    }

    val dubbingSettingsFlow: Flow<DubbingSettings> = safeData.map { prefs ->
        val providerStr = prefs[Keys.VOICE_PROVIDER] ?: AppConstants.PROVIDER_ANDROID_TTS
        val provider = VoiceProviderType.entries.firstOrNull { it.id == providerStr }
            ?: VoiceProviderType.ANDROID_TTS

        val policyStr = prefs[Keys.QUEUE_POLICY] ?: AudioQueuePolicy.REPLACE_CURRENT.name
        val policy = runCatching { AudioQueuePolicy.valueOf(policyStr) }
            .getOrDefault(AudioQueuePolicy.REPLACE_CURRENT)

        DubbingSettings(
            defaultProvider = provider,
            defaultVoiceId = prefs[Keys.VOICE_ID] ?: "",
            defaultPitch = (prefs[Keys.VOICE_PITCH] ?: 1.0f).coerceIn(0.5f, 2.0f),
            defaultSpeechRate = (prefs[Keys.VOICE_RATE] ?: 1.0f).coerceIn(0.5f, 2.0f),
            defaultVolume = (prefs[Keys.VOICE_VOLUME] ?: 1.0f).coerceIn(0.0f, 1.0f),
            voiceEnabled = prefs[Keys.VOICE_ENABLED] ?: true,
            autoPlay = prefs[Keys.AUTO_PLAY] ?: true,
            audioQueuePolicy = policy,
            sourceLanguageCode = prefs[Keys.SOURCE_LANG] ?: AppConstants.DEFAULT_SOURCE_LANGUAGE,
            targetLanguageCode = prefs[Keys.TARGET_LANG] ?: AppConstants.DEFAULT_TARGET_LANGUAGE,
            ocrIntervalMs = (prefs[Keys.OCR_INTERVAL_MS] ?: AppConstants.DEFAULT_FRAME_INTERVAL_MS).coerceIn(250L, 3000L),
            translationDelayMs = (prefs[Keys.TRANSLATION_DELAY_MS] ?: AppConstants.DEFAULT_TRANSLATION_DELAY_MS).coerceIn(0L, 1500L),
            ocrSensitivity = (prefs[Keys.OCR_SENSITIVITY] ?: AppConstants.DEFAULT_OCR_SENSITIVITY).coerceIn(0.4f, 0.98f),
            edgeEndpointUrl = prefs[Keys.EDGE_ENDPOINT_URL] ?: "",
            edgeBearerToken = prefs[Keys.EDGE_BEARER_TOKEN] ?: ""
        )
    }

    suspend fun saveRoiLayout(config: RoiLayoutConfig) {
        val name = config.nameRoi.clamped()
        val dialog = config.dialogRoi.clamped()
        context.autoDubDataStore.edit { prefs ->
            prefs[Keys.NAME_ROI_X] = name.leftFraction
            prefs[Keys.NAME_ROI_Y] = name.topFraction
            prefs[Keys.NAME_ROI_W] = name.widthFraction
            prefs[Keys.NAME_ROI_H] = name.heightFraction
            prefs[Keys.NAME_ROI_COLOR] = name.borderColorArgb
            prefs[Keys.NAME_ROI_ALPHA] = name.fillAlpha

            prefs[Keys.DIALOG_ROI_X] = dialog.leftFraction
            prefs[Keys.DIALOG_ROI_Y] = dialog.topFraction
            prefs[Keys.DIALOG_ROI_W] = dialog.widthFraction
            prefs[Keys.DIALOG_ROI_H] = dialog.heightFraction
            prefs[Keys.DIALOG_ROI_COLOR] = dialog.borderColorArgb
            prefs[Keys.DIALOG_ROI_ALPHA] = dialog.fillAlpha

            prefs[Keys.ROI_MODE] = config.mode.name
        }
    }

    suspend fun resetDefaultRoiLayout() {
        saveRoiLayout(
            RoiLayoutConfig(
                nameRoi = RoiBox.DEFAULT_NAME_ROI,
                dialogRoi = RoiBox.DEFAULT_DIALOG_ROI,
                mode = RoiMode.SETUP
            )
        )
    }

    suspend fun saveOverlayConfig(config: OverlayAppearanceConfig) {
        context.autoDubDataStore.edit { prefs ->
            prefs[Keys.OVERLAY_FONT_SIZE] = config.fontSizeSp.coerceIn(11f, 30f)
            prefs[Keys.OVERLAY_OPACITY] = config.backgroundOpacity.coerceIn(0.1f, 1.0f)
            prefs[Keys.OVERLAY_WIDTH] = config.widthFraction.coerceIn(0.4f, 0.98f)
            prefs[Keys.OVERLAY_TEXT_COLOR] = config.textColorArgb
            prefs[Keys.OVERLAY_OFFSET_X] = config.offsetXFraction.coerceIn(0f, 0.8f)
            prefs[Keys.OVERLAY_OFFSET_Y] = config.offsetYFraction.coerceIn(0f, 0.85f)
            prefs[Keys.OVERLAY_SHOW_ORIGINAL] = config.showOriginalText
            prefs[Keys.OVERLAY_VISIBLE] = config.isTranslationVisible
        }
    }

    suspend fun saveDubbingSettings(settings: DubbingSettings) {
        context.autoDubDataStore.edit { prefs ->
            prefs[Keys.VOICE_PROVIDER] = settings.defaultProvider.id
            prefs[Keys.VOICE_ID] = settings.defaultVoiceId
            prefs[Keys.VOICE_PITCH] = settings.defaultPitch.coerceIn(0.5f, 2.0f)
            prefs[Keys.VOICE_RATE] = settings.defaultSpeechRate.coerceIn(0.5f, 2.0f)
            prefs[Keys.VOICE_VOLUME] = settings.defaultVolume.coerceIn(0.0f, 1.0f)
            prefs[Keys.VOICE_ENABLED] = settings.voiceEnabled
            prefs[Keys.AUTO_PLAY] = settings.autoPlay
            prefs[Keys.QUEUE_POLICY] = settings.audioQueuePolicy.name
            prefs[Keys.SOURCE_LANG] = settings.sourceLanguageCode
            prefs[Keys.TARGET_LANG] = settings.targetLanguageCode
            prefs[Keys.OCR_INTERVAL_MS] = settings.ocrIntervalMs.coerceIn(250L, 3000L)
            prefs[Keys.TRANSLATION_DELAY_MS] = settings.translationDelayMs.coerceIn(0L, 1500L)
            prefs[Keys.OCR_SENSITIVITY] = settings.ocrSensitivity.coerceIn(0.4f, 0.98f)
            prefs[Keys.EDGE_ENDPOINT_URL] = settings.edgeEndpointUrl.trim()
            prefs[Keys.EDGE_BEARER_TOKEN] = settings.edgeBearerToken.trim()
        }
    }
}
