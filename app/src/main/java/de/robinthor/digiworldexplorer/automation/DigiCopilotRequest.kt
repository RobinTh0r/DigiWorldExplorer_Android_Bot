package de.robinthor.digiworldexplorer.automation

/** Explicit, process-local opt-in. Settings and an expired timer never start navigation alone. */
object DigiCopilotRequest {
    @Volatile private var enabled = false
    @Volatile var reason = ""
        private set

    @Synchronized fun start() { enabled = true; reason = "Waiting for verified Home" }
    @Synchronized fun stop(why: String = "") { enabled = false; reason = why }
    fun active() = enabled
}
