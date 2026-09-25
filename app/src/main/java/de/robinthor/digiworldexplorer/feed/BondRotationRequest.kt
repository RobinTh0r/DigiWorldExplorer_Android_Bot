package de.robinthor.digiworldexplorer.feed

/** One-shot user request. It bypasses mode/cooldown, but never screen verification. */
object BondRotationRequest {
    @Volatile private var requested = false
    @Volatile var reason = ""
        private set

    @Synchronized fun start() {
        BondRotationAnalyzer.reset()
        requested = true
        reason = "Waiting for verified Home"
    }

    @Synchronized fun complete() { requested = false; reason = "Bond rotation complete" }
    @Synchronized fun park(why: String) { requested = false; reason = why }
    @Synchronized fun cancel() { requested = false; reason = "" }
    fun active() = requested
}
