package de.robinthor.digiworldexplorer.automation

enum class AutomationEventKind {
    CAPTURE_STARTED, CAPTURE_STOPPED, OWNER_CHANGED, ACTION_DISPATCHED,
    ACTION_VERIFIED, TIMEOUT, PARKED,
}

data class AutomationEvent(val elapsedMillis: Long, val kind: AutomationEventKind, val code: String)

/** Bounded non-sensitive diagnostic ring; raw frames and user identifiers never belong here. */
object AutomationEventLog {
    private const val CAPACITY = 200
    private val events = ArrayDeque<AutomationEvent>(CAPACITY)

    @Synchronized
    fun record(kind: AutomationEventKind, code: String, elapsedMillis: Long = android.os.SystemClock.elapsedRealtime()) {
        require(code.length <= 80) { "Diagnostic code must stay compact" }
        require(code.all { it.isLetterOrDigit() || it in "_:-." }) { "Diagnostic code contains unsafe characters" }
        if (events.size == CAPACITY) events.removeFirst()
        events.addLast(AutomationEvent(elapsedMillis, kind, code))
    }

    @Synchronized fun snapshot(): List<AutomationEvent> = events.toList()
    @Synchronized fun clear() = events.clear()
}
