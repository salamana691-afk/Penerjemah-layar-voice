package com.autodub.edge

import com.autodub.edge.engine.voice.SSMLGenerator
import com.autodub.edge.engine.voice.SsmlConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SSMLGeneratorTest {

    @Test
    fun `generate escapes XML special characters and prevents injection`() {
        val ssml = SSMLGenerator.generate(
            text = "Hello <Alex> & \"Nora\" 'Test'",
            config = SsmlConfig(
                voiceId = "id-ID-GadisNeural",
                pitchMultiplier = 1.10f,
                rateMultiplier = 1.05f
            )
        )
        assertTrue(ssml.contains("&lt;Alex&gt; &amp; &quot;Nora&quot; &apos;Test&apos;"))
        assertTrue(ssml.contains("<voice name=\"id-ID-GadisNeural\">"))
        assertTrue(ssml.contains("pitch=\"+10%\""))
        assertTrue(ssml.contains("rate=\"+5%\""))
    }

    @Test
    fun `invalid voice id injection is stripped safely`() {
        val ssml = SSMLGenerator.generate(
            text = "Halo dunia",
            config = SsmlConfig(
                voiceId = "bad\"><script>alert(1)</script>",
                pitchMultiplier = 0.95f,
                rateMultiplier = 0.90f
            )
        )
        assertFalse(ssml.contains("<script>"))
        assertTrue(ssml.contains("pitch=\"-5%\""))
        assertTrue(ssml.contains("rate=\"-10%\""))
    }

    @Test
    fun `plain speech fallback clamps pitch and rate within bounds`() {
        val plain = SSMLGenerator.toPlainSpeechParams(
            text = "  Tes suara  ",
            config = SsmlConfig(
                voiceId = "id-ID-ArdiNeural",
                pitchMultiplier = 5.0f,
                rateMultiplier = 0.1f
            )
        )
        assertEqualsSafe(2.0f, plain.pitch)
        assertEqualsSafe(0.5f, plain.rate)
    }

    private fun assertEqualsSafe(expected: Float, actual: Float) {
        assertTrue(kotlin.math.abs(expected - actual) < 0.001f)
    }
}
