package com.autodub.edge

import com.autodub.edge.core.util.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `normalizeCharacterName strips brackets spaces and normalizes case`() {
        assertEquals("NORA", TextNormalizer.normalizeCharacterName("[ Nora ]"))
        assertEquals("SALMAN", TextNormalizer.normalizeCharacterName("  salman : "))
        assertEquals("DR. ALEX", TextNormalizer.normalizeCharacterName("| Dr.   Alex |"))
        assertEquals("", TextNormalizer.normalizeCharacterName("   "))
    }

    @Test
    fun `normalizeDialogueText preserves reading order and joins soft wraps`() {
        val raw = "I didn't expect to\nfind you here in the\nlibrary tonight."
        val expected = "I didn't expect to find you here in the library tonight."
        assertEquals(expected, TextNormalizer.normalizeDialogueText(raw))
    }

    @Test
    fun `normalizeDialogueText rejoins hyphenated line breaks and removes garbage`() {
        val raw = "This is un-\nbelievable!\n..."
        assertEquals("This is unbelievable!", TextNormalizer.normalizeDialogueText(raw))
    }

    @Test
    fun `similarity detects close matches accurately`() {
        val sim = TextNormalizer.similarity("NORA", "N0RA")
        assertTrue(sim >= 0.75)
        assertEquals(1.0, TextNormalizer.similarity("SALMAN", "SALMAN"), 0.001)
    }
}
