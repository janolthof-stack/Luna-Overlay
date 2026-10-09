package de.olti.lunaoverlay

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatGptStateDetectorTest {
    @Test fun missingSignalsRemainUnknown() {
        assertEquals(ChatGptStateDetector.State.UNKNOWN, ChatGptStateDetector.detect(emptyList()))
        assertEquals(ChatGptStateDetector.State.UNKNOWN, ChatGptStateDetector.detect(listOf("Mute microphone", "End voice chat")))
    }
    @Test fun explicitControlsAreRecognized() {
        assertEquals(ChatGptStateDetector.State.THINKING, ChatGptStateDetector.detect(listOf("Stop generating")))
        assertEquals(ChatGptStateDetector.State.SPEAKING, ChatGptStateDetector.detect(listOf("Vorlesen stoppen")))
        assertEquals(ChatGptStateDetector.State.LISTENING, ChatGptStateDetector.detect(listOf(" Listening… ")))
    }
    @Test fun conflictingSignalsRemainUnknown() {
        assertEquals(ChatGptStateDetector.State.UNKNOWN, ChatGptStateDetector.detect(listOf("Listening", "Speaking")))
    }
    @Test fun quotedPhrasesAreNotSignals() {
        assertEquals(ChatGptStateDetector.State.UNKNOWN, ChatGptStateDetector.detect(listOf("Please press Stop generating", "Why are you speaking?")))
    }
    @Test fun activeStateWinsOverGenericReadyLabel() {
        assertEquals(ChatGptStateDetector.State.SPEAKING, ChatGptStateDetector.detect(listOf("Ready", "Stop reading aloud")))
    }
}
