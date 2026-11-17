package com.autodub.edge.engine.voice

import kotlin.math.roundToInt

data class SsmlConfig(
    val voiceId: String,
    val pitchMultiplier: Float,
    val rateMultiplier: Float,
    val languageTag: String = "id-ID",
    val style: String = ""
)

data class PlainSpeechParams(
    val text: String,
    val voiceId: String,
    val pitch: Float,
    val rate: Float
)

object SSMLGenerator {

    private val validVoiceIdRegex = Regex("^[A-Za-z0-9._:-]{2,96}$")
    private val validLanguageTagRegex = Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})?$")
    private val validStyleRegex = Regex("^[A-Za-z0-9_-]{1,32}$")

    fun generate(text: String, config: SsmlConfig): String {
        val cleanText = escapeXml(text.trim())
        val safeVoiceId = sanitizeVoiceId(config.voiceId)
        val safeLang = sanitizeLanguageTag(config.languageTag)
        val pitchStr = formatRelativePercentage(config.pitchMultiplier, min = 0.5f, max = 2.0f)
        val rateStr = formatRelativePercentage(config.rateMultiplier, min = 0.5f, max = 2.0f)
        val safeStyle = config.style.trim().takeIf { validStyleRegex.matches(it) }

        val prosodyBlock = "<prosody pitch=\"$pitchStr\" rate=\"$rateStr\">$cleanText</prosody>"
        val innerContent = if (safeStyle != null) {
            "<mstts:express-as style=\"$safeStyle\">$prosodyBlock</mstts:express-as>"
        } else {
            prosodyBlock
        }

        val voiceBlock = if (safeVoiceId.isNotEmpty()) {
            "<voice name=\"${escapeXml(safeVoiceId)}\">$innerContent</voice>"
        } else {
            innerContent
        }

        val namespaceDecl = if (safeStyle != null) {
            " xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"https://www.w3.org/2001/mstts\""
        } else {
            " xmlns=\"http://www.w3.org/2001/10/synthesis\""
        }

        return "<speak version=\"1.0\"$namespaceDecl xml:lang=\"$safeLang\">$voiceBlock</speak>"
    }

    fun toPlainSpeechParams(text: String, config: SsmlConfig): PlainSpeechParams {
        return PlainSpeechParams(
            text = text.trim(),
            voiceId = sanitizeVoiceId(config.voiceId),
            pitch = config.pitchMultiplier.coerceIn(0.5f, 2.0f),
            rate = config.rateMultiplier.coerceIn(0.5f, 2.0f)
        )
    }

    fun escapeXml(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length + 16)
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun sanitizeVoiceId(voiceId: String): String {
        val trimmed = voiceId.trim()
        return if (validVoiceIdRegex.matches(trimmed)) trimmed else ""
    }

    fun sanitizeLanguageTag(tag: String): String {
        val trimmed = tag.trim()
        return if (validLanguageTagRegex.matches(trimmed)) trimmed else "id-ID"
    }

    /**
     * Converts multiplier (e.g. 1.10f -> "+10%", 0.95f -> "-5%", 1.0f -> "0%").
     */
    fun formatRelativePercentage(multiplier: Float, min: Float = 0.5f, max: Float = 2.0f): String {
        val clamped = multiplier.coerceIn(min, max)
        val deltaPct = ((clamped - 1.0f) * 100f).roundToInt()
        return when {
            deltaPct > 0 -> "+$deltaPct%"
            deltaPct < 0 -> "$deltaPct%"
            else -> "0%"
        }
    }
}
