package com.autodub.edge

import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.CharacterProfile
import com.autodub.edge.domain.usecase.CharacterMatchResult
import com.autodub.edge.domain.usecase.CharacterMatcher
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterMatcherTest {

    private val profiles = listOf(
        CharacterProfile(
            id = 1L,
            characterName = AppConstants.DEFAULT_NARRATOR_NAME,
            voiceId = "default-narrator"
        ),
        CharacterProfile(
            id = 2L,
            characterName = "NORA",
            voiceId = "id-ID-GadisNeural",
            pitch = 1.10f,
            speechRate = 1.05f
        ),
        CharacterProfile(
            id = 3L,
            characterName = "SALMAN",
            voiceId = "id-ID-ArdiNeural",
            pitch = 0.95f,
            speechRate = 0.95f
        )
    )

    @Test
    fun `exact match resolves character voice configuration`() {
        val matcher = CharacterMatcher()
        val result = matcher.match("  Nora ", profiles)
        assertEquals(CharacterMatchResult.MatchType.EXACT, result.matchType)
        assertEquals("id-ID-GadisNeural", result.profile.voiceId)
    }

    @Test
    fun `minor OCR typo resolves conservatively without confusing distinct characters`() {
        val matcher = CharacterMatcher(minFuzzyThreshold = 0.75)
        val result = matcher.match("SALM4N", profiles)
        assertEquals(CharacterMatchResult.MatchType.FUZZY, result.matchType)
        assertEquals("SALMAN", result.profile.characterName)
    }

    @Test
    fun `missing name preserves previous speaker when available`() {
        val matcher = CharacterMatcher()
        matcher.match("NORA", profiles)
        val preserved = matcher.match("", profiles)
        assertEquals(CharacterMatchResult.MatchType.PREVIOUS_SPEAKER_PRESERVED, preserved.matchType)
        assertEquals("NORA", preserved.profile.characterName)
    }

    @Test
    fun `unknown character falls back to Default Narrator`() {
        val matcher = CharacterMatcher()
        val result = matcher.match("MYSTERIOUS STRANGER", profiles)
        assertEquals(CharacterMatchResult.MatchType.DEFAULT_NARRATOR, result.matchType)
        assertEquals(AppConstants.DEFAULT_NARRATOR_NAME, result.profile.characterName)
    }
}
