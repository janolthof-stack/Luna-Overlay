package de.olti.lunaoverlay

import java.util.Locale

/** Only UI controls/status labels may enter this detector, never conversation text. */
object ChatGptStateDetector {
    enum class State { IDLE, LISTENING, THINKING, SPEAKING, UNKNOWN }

    private val listening = setOf("listening", "listening...", "listening…", "ich höre zu", "ich höre zu…", "zuhören")
    private val thinking = setOf("thinking", "thinking...", "thinking…", "denke nach", "denke nach…", "stop generating", "generierung stoppen", "antwortgenerierung stoppen")
    private val speaking = setOf("speaking", "speaking...", "speaking…", "spricht", "stop reading", "stop reading aloud", "vorlesen stoppen", "vorlesen beenden")
    private val idle = setOf("ready", "bereit", "idle", "ruhezustand")

    fun detect(labels: Collection<String>): State {
        val normalized = labels.map { it.trim().lowercase(Locale.ROOT) }.toSet()
        val active = mutableSetOf<State>()
        if (normalized.any { it in listening }) active.add(State.LISTENING)
        if (normalized.any { it in thinking }) active.add(State.THINKING)
        if (normalized.any { it in speaking }) active.add(State.SPEAKING)
        // Conflicting or unavailable signals must never invent a conversation state.
        if (active.size > 1) return State.UNKNOWN
        return active.firstOrNull() ?: if (normalized.any { it in idle }) State.IDLE else State.UNKNOWN
    }
}
