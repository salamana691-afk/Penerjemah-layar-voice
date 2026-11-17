package com.autodub.edge

import com.autodub.edge.domain.usecase.DuplicateDialogueDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDialogueDetectorTest {

    @Test
    fun `identical speaker and dialogue is rejected as duplicate`() {
        val detector = DuplicateDialogueDetector(debounceWindowMs = 1500L)
        assertTrue(detector.isNewDialogue("NORA", "Welcome to the library.", nowMs = 1000L))
        assertFalse(detector.isNewDialogue("NORA", "Welcome to the library.", nowMs = 1300L))
    }

    @Test
    fun `same dialogue spoken by different speaker is treated as new`() {
        val detector = DuplicateDialogueDetector(debounceWindowMs = 1500L)
        assertTrue(detector.isNewDialogue("NORA", "Wait a second!", nowMs = 1000L))
        assertTrue(detector.isNewDialogue("SALMAN", "Wait a second!", nowMs = 1200L))
    }

    @Test
    fun `minor OCR jitter within debounce window is suppressed`() {
        val detector = DuplicateDialogueDetector(debounceWindowMs = 1500L)
        assertTrue(detector.isNewDialogue("NORA", "Where did everyone go tonight?", nowMs = 1000L))
        assertFalse(detector.isNewDialogue("NORA", "Where did everyone go tonight", nowMs = 1400L))
    }

    @Test
    fun `genuinely new dialogue passes immediately`() {
        val detector = DuplicateDialogueDetector(debounceWindowMs = 1500L)
        assertTrue(detector.isNewDialogue("NORA", "Good morning.", nowMs = 1000L))
        assertTrue(detector.isNewDialogue("NORA", "Did you sleep well?", nowMs = 1300L))
    }
}
