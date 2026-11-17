package com.autodub.edge.engine.voice

import com.autodub.edge.domain.model.VoiceEmotion
import com.autodub.edge.domain.model.VoicePersonality

object VoicePromptBuilder {

    /**
     * Builds an expressive voice-design style instruction for AI Neural TTS (e.g. Gemini 3.8 TTS)
     * combining anime-inspired persona, emotional state, custom user description, and prosody hints.
     */
    fun buildStylePrompt(
        personality: VoicePersonality,
        emotion: VoiceEmotion,
        customDescription: String,
        pitch: Float,
        speechRate: Float
    ): String {
        val parts = mutableListOf<String>()

        val custom = customDescription.trim()
        if (custom.isNotEmpty()) {
            parts.add(custom)
        } else if (personality.stylePrompt.isNotEmpty()) {
            parts.add(personality.stylePrompt)
        }

        if (personality != VoicePersonality.CUSTOM_ONLY && custom.isNotEmpty() && personality.stylePrompt.isNotEmpty()) {
            parts.add("Karakter dasar: ${personality.displayName} (${personality.genderLabel}).")
        }

        parts.add("Emosi: ${emotion.displayName} — ${emotion.styleDirective}")

        val pitchNote = when {
            pitch >= 1.15f -> "Nada suara sedikit lebih tinggi dan cerah."
            pitch <= 0.88f -> "Nada suara sedikit lebih rendah dan dalam."
            else -> ""
        }
        val rateNote = when {
            speechRate >= 1.15f -> "Tempo bicara agak cepat namun tetap jelas."
            speechRate <= 0.88f -> "Tempo bicara lebih santai dan tenang."
            else -> ""
        }

        if (pitchNote.isNotEmpty()) parts.add(pitchNote)
        if (rateNote.isNotEmpty()) parts.add(rateNote)

        parts.add("Bacakan dialog berikut secara natural dalam bahasa Indonesia dengan artikulasi jernih tanpa membaca instruksi ini.")
        return parts.joinToString(" ")
    }

    /**
     * Computes effective Android Native TTS pitch incorporating character pitch + emotion delta.
     */
    fun computeOfflinePitch(basePitch: Float, emotion: VoiceEmotion): Float {
        return (basePitch + emotion.offlinePitchDelta).coerceIn(0.5f, 2.0f)
    }

    /**
     * Computes effective Android Native TTS speech rate incorporating character rate + emotion delta.
     */
    fun computeOfflineRate(baseRate: Float, emotion: VoiceEmotion): Float {
        return (baseRate + emotion.offlineRateDelta).coerceIn(0.5f, 2.0f)
    }
}
