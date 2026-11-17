package com.autodub.edge.core.util

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object TextNormalizer {

    private val whitespaceRegex = Regex("\\s+")
    private val nameNoiseRegex = Regex("[\\[\\](){}<>|_~`*=•·:;\"]+")
    private val dialogGarbageLineRegex = Regex("^[\\W_]{1,3}$")
    private val multiSpaceRegex = Regex("[ \\t]{2,}")

    /**
     * Normalizes an OCR-detected character nameplate string.
     * Strips surrounding brackets/punctuation, collapses whitespace, and normalizes to UPPERCASE
     * for consistent Room profile matching.
     */
    fun normalizeCharacterName(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val stripped = nameNoiseRegex.replace(raw, " ")
        val collapsed = whitespaceRegex.replace(stripped, " ").trim()
        if (collapsed.length < 2) return ""
        // Keep alphanumeric, spaces, apostrophes, hyphens, and dots
        val clean = collapsed.filter { it.isLetterOrDigit() || it == ' ' || it == '\'' || it == '-' || it == '.' }
            .trim()
        return clean.uppercase(Locale.ROOT)
    }

    /**
     * Formats normalized key into clean display title case (e.g., "NORA" -> "Nora").
     */
    fun toDisplayName(normalized: String): String {
        if (normalized.isBlank()) return ""
        return normalized.lowercase(Locale.ROOT)
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
                }
            }
    }

    /**
     * Cleans multi-line Visual Novel dialog OCR text while preserving natural reading order.
     * Joins soft-wrapped lines within a paragraph cleanly and strips isolated OCR artifacts.
     */
    fun normalizeDialogueText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val lines = raw.replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { line ->
                multiSpaceRegex.replace(
                    line.replace('|', 'I')
                        .replace("“", "\"")
                        .replace("”", "\"")
                        .replace("’", "'")
                        .trim(),
                    " "
                )
            }
            .filter { line ->
                line.isNotBlank() && !dialogGarbageLineRegex.matches(line)
            }

        if (lines.isEmpty()) return ""

        val joined = StringBuilder()
        for (i in lines.indices) {
            val current = lines[i]
            if (joined.isNotEmpty()) {
                val prevChar = joined.last()
                if (prevChar == '-') {
                    // Rejoin hyphenated word across line break
                    joined.deleteCharAt(joined.length - 1)
                } else {
                    joined.append(' ')
                }
            }
            joined.append(current)
        }
        return multiSpaceRegex.replace(joined.toString(), " ").trim()
    }

    /**
     * Normalized similarity score in [0.0, 1.0] using Levenshtein edit distance.
     */
    fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val distance = levenshteinDistance(a, b)
        val maxLen = max(a.length, b.length)
        return (1.0 - distance.toDouble() / maxLen.toDouble()).coerceIn(0.0, 1.0)
    }

    fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val len0 = lhs.length + 1
        val len1 = rhs.length + 1
        var cost = IntArray(len0) { it }
        var newCost = IntArray(len0)

        for (j in 1 until len1) {
            newCost[0] = j
            for (i in 1 until len0) {
                val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                val costReplace = cost[i - 1] + match
                val costInsert = cost[i] + 1
                val costDelete = newCost[i - 1] + 1
                newCost[i] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[len0 - 1]
    }
}
