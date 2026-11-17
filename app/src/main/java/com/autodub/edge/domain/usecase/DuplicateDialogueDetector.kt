package com.autodub.edge.domain.usecase

import com.autodub.edge.core.util.TextNormalizer

class DuplicateDialogueDetector(
    private val debounceWindowMs: Long = 1500L,
    private val historyCapacity: Int = 10
) {
    private var lastSignature: String = ""
    private var lastAcceptedTimestampMs: Long = 0L
    private val recentSignatures = ArrayDeque<String>(historyCapacity)

    /**
     * Evaluates whether the (speaker, dialog) pair represents a genuinely new dialogue line.
     * Combines speaker identity and cleaned dialog text, ignoring minor OCR punctuation jitter
     * within the debounce window.
     */
    @Synchronized
    fun isNewDialogue(
        speakerKey: String,
        dialogueText: String,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val cleanDialog = TextNormalizer.normalizeDialogueText(dialogueText)
        if (cleanDialog.length < 2) return false

        val cleanSpeaker = TextNormalizer.normalizeCharacterName(speakerKey)
        val signature = "$cleanSpeaker::$cleanDialog"

        if (signature == lastSignature) {
            return false
        }

        // Reject minor OCR character flicker on the exact same speaker within debounce window
        if (lastSignature.startsWith("$cleanSpeaker::") && (nowMs - lastAcceptedTimestampMs) < debounceWindowMs) {
            val previousDialog = lastSignature.substringAfter("::")
            val similarity = TextNormalizer.similarity(previousDialog, cleanDialog)
            if (similarity >= 0.94) {
                return false
            }
        }

        // Prevent immediate oscillation back to a recently emitted frame
        if (recentSignatures.contains(signature) && (nowMs - lastAcceptedTimestampMs) < debounceWindowMs * 2) {
            return false
        }

        lastSignature = signature
        lastAcceptedTimestampMs = nowMs
        if (recentSignatures.size >= historyCapacity) {
            recentSignatures.removeFirst()
        }
        recentSignatures.addLast(signature)
        return true
    }

    @Synchronized
    fun reset() {
        lastSignature = ""
        lastAcceptedTimestampMs = 0L
        recentSignatures.clear()
    }
}
