package com.autodub.edge.domain.usecase

import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.core.util.TextNormalizer
import com.autodub.edge.domain.model.CharacterProfile

data class CharacterMatchResult(
    val profile: CharacterProfile,
    val detectedSpeakerName: String,
    val matchType: MatchType
) {
    enum class MatchType {
        EXACT,
        FUZZY,
        PREVIOUS_SPEAKER_PRESERVED,
        DEFAULT_NARRATOR
    }
}

class CharacterMatcher(
    private val minFuzzyThreshold: Double = 0.80
) {
    private var lastMatchedSpeakerName: String = ""

    fun resetSession() {
        lastMatchedSpeakerName = ""
    }

    /**
     * Matches raw OCR character name against Room character profiles.
     * - Normalizes OCR text.
     * - If OCR temporarily fails to detect a name (blank), preserves previous speaker when available,
     *   otherwise falls back to Default Narrator.
     * - Uses conservative fuzzy matching (>= 0.80 similarity & unique gap >= 0.12 over runner-up)
     *   so distinct characters with similar names are not accidentally confused.
     */
    fun match(
        rawOcrName: String?,
        profiles: List<CharacterProfile>
    ): CharacterMatchResult {
        val defaultNarrator = profiles.firstOrNull {
            it.characterName.equals(AppConstants.DEFAULT_NARRATOR_NAME, ignoreCase = true)
        } ?: CharacterProfile(
            characterName = AppConstants.DEFAULT_NARRATOR_NAME,
            voiceProvider = AppConstants.PROVIDER_ANDROID_TTS,
            pitch = 1.0f,
            speechRate = 1.0f,
            volume = 1.0f,
            enabled = true
        )

        val normalized = TextNormalizer.normalizeCharacterName(rawOcrName)
        if (normalized.isBlank()) {
            if (lastMatchedSpeakerName.isNotBlank()) {
                val previousProfile = profiles.firstOrNull {
                    it.characterName.equals(lastMatchedSpeakerName, ignoreCase = true)
                }
                if (previousProfile != null) {
                    return CharacterMatchResult(
                        profile = previousProfile,
                        detectedSpeakerName = TextNormalizer.toDisplayName(previousProfile.characterName),
                        matchType = CharacterMatchResult.MatchType.PREVIOUS_SPEAKER_PRESERVED
                    )
                }
            }
            return CharacterMatchResult(
                profile = defaultNarrator,
                detectedSpeakerName = AppConstants.DEFAULT_NARRATOR_DISPLAY,
                matchType = CharacterMatchResult.MatchType.DEFAULT_NARRATOR
            )
        }

        val namedProfiles = profiles.filter {
            !it.characterName.equals(AppConstants.DEFAULT_NARRATOR_NAME, ignoreCase = true)
        }

        // 1. Exact case-insensitive match
        val exact = namedProfiles.firstOrNull {
            it.characterName.equals(normalized, ignoreCase = true)
        }
        if (exact != null) {
            lastMatchedSpeakerName = exact.characterName
            return CharacterMatchResult(
                profile = exact,
                detectedSpeakerName = TextNormalizer.toDisplayName(exact.characterName),
                matchType = CharacterMatchResult.MatchType.EXACT
            )
        }

        // 2. Conservative fuzzy match (minimum length 3 to avoid short-token collisions)
        if (normalized.length >= 3 && namedProfiles.isNotEmpty()) {
            val scored = namedProfiles.map { profile ->
                profile to TextNormalizer.similarity(normalized, profile.characterName.uppercase())
            }.sortedByDescending { it.second }

            val best = scored.first()
            val runnerUpScore = scored.getOrNull(1)?.second ?: 0.0
            val hasClearMargin = (best.second - runnerUpScore) >= 0.10 || scored.size == 1

            if (best.second >= minFuzzyThreshold && hasClearMargin) {
                lastMatchedSpeakerName = best.first.characterName
                return CharacterMatchResult(
                    profile = best.first,
                    detectedSpeakerName = TextNormalizer.toDisplayName(best.first.characterName),
                    matchType = CharacterMatchResult.MatchType.FUZZY
                )
            }
        }

        // 3. Unknown character -> record speaker display name, use Default Narrator voice config
        lastMatchedSpeakerName = normalized
        return CharacterMatchResult(
            profile = defaultNarrator,
            detectedSpeakerName = TextNormalizer.toDisplayName(normalized),
            matchType = CharacterMatchResult.MatchType.DEFAULT_NARRATOR
        )
    }
}
