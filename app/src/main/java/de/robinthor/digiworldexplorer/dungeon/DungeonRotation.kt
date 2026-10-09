package de.robinthor.digiworldexplorer.dungeon

import android.content.Context
import android.os.SystemClock
import android.util.Log

/** User-started dungeon pass. It is deliberately separate from the already-open VS/Tower loop. */
object DungeonRotationRequest {
    enum class Phase { IDLE, REQUESTED, OPENING_LIST, SURVEYING, RUNNING, PARKED, COMPLETE }

    @Volatile var phase = Phase.IDLE
        private set
    @Volatile var reason = ""
        private set
    @Volatile var requestedAt = 0L
        private set
    @Volatile var completedToday: Set<DungeonKey> = emptySet()
        private set
    @Volatile var apocalymonStatus = "Apocalymon disabled"
        private set
    private var suspendedNetworkDefense = false

    @Synchronized fun start(context: Context) {
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.initialize(context)
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("DUNGEON", "start requested")
        if (de.robinthor.digiworldexplorer.license.SupporterLicenseManager.load(context) == null ||
            !de.robinthor.digiworldexplorer.strategy.AutomationState.enabled) return
        DungeonRotationAnalyzer.reset()
        DungeonFrameAnalyzer.reset()
        de.robinthor.digiworldexplorer.network.NetworkDefenseFrameAnalyzer.reset()
        suspendedNetworkDefense = de.robinthor.digiworldexplorer.strategy.AutomationState.autoNetworkDefenseEnabled
        de.robinthor.digiworldexplorer.strategy.AutomationState.autoNetworkDefenseEnabled = false
        de.robinthor.digiworldexplorer.strategy.AutoMoveController.pauseForPurchaseScreen()
        de.robinthor.digiworldexplorer.feed.BondRotationRequest.cancel()
        de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.stop("Dungeon Co-Pilot started")
        de.robinthor.digiworldexplorer.feed.BondRotationAnalyzer.reset()
        de.robinthor.digiworldexplorer.feed.FeedFrameAnalyzer.reset()
        de.robinthor.digiworldexplorer.strategy.AutomationState.adSkipPassEnabled =
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("ad_skip_pass", false)
        completedToday = DungeonPassPolicy.locked(DungeonDailyStore.snapshot(context))
        val diagnosticSettings = DungeonSettingsStore.load(context)
        val diagnosticDay = DungeonDailyStore.snapshot(context)
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.snapshotContext("dungeon-start", mapOf(
            "gameDay" to diagnosticDay.gameDay.toString(), "dailyProgress" to diagnosticDay.progress.toString(),
            "completedDaily" to completedToday.toString()))
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record(
            "DUNGEON.CONFIG", "adSkipPass=${de.robinthor.digiworldexplorer.strategy.AutomationState.adSkipPassEnabled} useAds=${diagnosticSettings.useAdAttempts} normal=${diagnosticSettings.normalAttempts} enabled=${diagnosticSettings.enabledCards}")
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record(
            "DUNGEON.LEDGER", "day=${diagnosticDay.gameDay} progress=${diagnosticDay.progress}")
        val apocalymonEnabled = DungeonKey.APOCALYMON_WALL in DungeonSettingsStore.load(context).enabledCards
        apocalymonStatus = when {
            !apocalymonEnabled -> "Apocalymon disabled"
            DungeonKey.APOCALYMON_WALL in completedToday -> "Apocalymon already attempted today"
            else -> "Apocalymon queued"
        }
        phase = Phase.REQUESTED
        reason = if (DungeonKey.APOCALYMON_WALL in completedToday)
            "Waiting for Home; Apocalymon already complete today" else "Waiting for verified Home"
        requestedAt = SystemClock.elapsedRealtime()
        Log.i("DigiWorldDungeonRotation", "START enabled=${DungeonSettingsStore.load(context).enabledCards} locked=$completedToday apocalymon=$apocalymonStatus")
    }

    @Synchronized fun openingList() {
        if (phase == Phase.REQUESTED) {
            phase = Phase.OPENING_LIST
            reason = "Opening dungeon list"
            de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("DUNGEON", "phase=$phase reason=$reason")
        }
    }

    @Synchronized fun listVerified() {
        if (phase == Phase.REQUESTED || phase == Phase.OPENING_LIST) {
            phase = Phase.SURVEYING
            reason = "Dungeon list verified"
            de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("DUNGEON", "phase=$phase reason=$reason")
        }
    }

    @Synchronized fun park(why: String) { phase = Phase.PARKED; reason = why; de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("DUNGEON", "phase=$phase reason=$reason") }
    @Synchronized fun complete() {
        phase = Phase.COMPLETE; reason = "Daily pass complete"
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("DUNGEON", "phase=$phase reason=$reason")
        restoreNetworkDefense()
    }
    @Synchronized fun cancel() {
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("DUNGEON", "cancel phase=$phase reason=$reason")
        phase = Phase.IDLE; reason = ""; requestedAt = 0L; completedToday = emptySet(); apocalymonStatus = "Apocalymon disabled"
        restoreNetworkDefense()
    }
    private fun restoreNetworkDefense() {
        de.robinthor.digiworldexplorer.strategy.AutomationState.autoNetworkDefenseEnabled = suspendedNetworkDefense
        suspendedNetworkDefense = false
        de.robinthor.digiworldexplorer.network.NetworkDefenseFrameAnalyzer.reset()
    }
    fun active() = phase in setOf(Phase.REQUESTED, Phase.OPENING_LIST, Phase.SURVEYING, Phase.RUNNING)
    fun ownsFrames() = active() || phase == Phase.PARKED
}

/** Safe defaults: visible counters remain authoritative; these are hard ceilings, never targets. */
object DungeonRotationPolicy {
    val budgets = mapOf(
        DungeonKey.APOCALYMON_WALL to DungeonBudget(attempts = 1),
        DungeonKey.DEMIDEVIMON to DungeonBudget(attempts = 4, adTickets = 2),
        DungeonKey.BAKEMON to DungeonBudget(attempts = 4, adTickets = 2),
        DungeonKey.DIGIFACTORY to DungeonBudget(attempts = 4, adTickets = 2),
        DungeonKey.NETWORK_DEFENSE to DungeonBudget(attempts = 4, adTickets = 2),
        DungeonKey.METAL_SEA to DungeonBudget(attempts = 4, adTickets = 2),
        // VS is opened once; its subsequent automatic battles stay in the existing VS module.
        DungeonKey.DAILY to DungeonBudget(attempts = 1),
    )

    fun adAllowed(key: DungeonKey, adSkipPass: Boolean) =
        adSkipPass && (budgets[key]?.adTickets ?: 0) > 0 && key != DungeonKey.APOCALYMON_WALL
}
