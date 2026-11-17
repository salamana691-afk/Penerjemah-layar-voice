package com.autodub.edge

import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.AudioQueuePolicy
import com.autodub.edge.domain.model.CharacterProfile
import com.autodub.edge.domain.model.DubbingSettings
import com.autodub.edge.domain.model.RoiBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAndAudioQueueTest {

    @Test
    fun `roi box clamping keeps coordinates inside viewport bounds`() {
        val box = RoiBox(
            leftFraction = 0.95f,
            topFraction = 0.95f,
            widthFraction = 0.30f,
            heightFraction = 0.25f,
            borderColorArgb = AppConstants.DEFAULT_NAME_ROI_COLOR,
            fillAlpha = 1.5f
        ).clamped()

        assertTrue(box.leftFraction + box.widthFraction <= 1.0001f)
        assertTrue(box.topFraction + box.heightFraction <= 1.0001f)
        assertTrue(box.fillAlpha <= 0.85f)
    }

    @Test
    fun `default dubbing settings use Replace Current queue policy`() {
        val settings = DubbingSettings()
        assertEquals(AudioQueuePolicy.REPLACE_CURRENT, settings.audioQueuePolicy)
        assertEquals("en", settings.sourceLanguageCode)
        assertEquals("id", settings.targetLanguageCode)
    }

    @Test
    fun `character voice configuration retains custom pitch and rate`() {
        val nora = CharacterProfile(
            characterName = "NORA",
            voiceProvider = AppConstants.PROVIDER_EDGE_COMPATIBLE,
            voiceId = "id-ID-GadisNeural",
            pitch = 1.10f,
            speechRate = 1.05f,
            volume = 0.90f,
            enabled = true
        )
        assertEquals("id-ID-GadisNeural", nora.voiceId)
        assertEquals(1.10f, nora.pitch, 0.001f)
        assertEquals(1.05f, nora.speechRate, 0.001f)
    }
}
