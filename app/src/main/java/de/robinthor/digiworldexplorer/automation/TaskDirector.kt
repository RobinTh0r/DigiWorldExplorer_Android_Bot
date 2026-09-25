package de.robinthor.digiworldexplorer.automation

enum class TaskKey { FARM, BOND_TOUR, DUNGEON_ROTATION, RUNNER, WORLD_SEARCH, SUMMON, TOWER, QUESTS, PASSIVE }
enum class TaskPhase { IDLE, WAITING_HOME, NAVIGATING, WORKING, RETURNING_HOME, WAITING_DUE, COMPLETE, PARKED, STOPPED }
enum class TaskOutcome { COMPLETE, NOTHING_DUE, STOPPED, TIMED_OUT, UNSAFE }
data class TaskSchedule(val key: TaskKey, val enabled: Boolean = false, val dueAtMillis: Long = 0, val remainingBudget: Int = 1)
data class AutomationSnapshot(val phase: TaskPhase = TaskPhase.IDLE, val task: TaskKey? = null, val reason: String = "IDLE", val completed: Set<TaskKey> = emptySet())
enum class DirectorOperation { WAIT, NAVIGATE, WORK, RETURN_HOME, COMPLETE, PARK }
data class DirectorCommand(val operation: DirectorOperation, val task: TaskKey? = null)

/**
 * Expedition scheduler with explicit completion and home-return handshakes. It never infers that
 * a task finished from a timeout or grants work on an unknown screen. External adapters own screen
 * recognition and dispatch; until those exist this scheduler is deliberately not live in capture.
 */
class TaskDirector(private val navigationTimeoutMillis: Long = 20_000) {
    var snapshot = AutomationSnapshot()
        private set
    private var mode = AutomationMode.SEMI_AUTO
    private var repeat = false
    private var deadline = 0L
    private var deadlineStarted = false

    init { require(navigationTimeoutMillis > 0) }

    fun start(mode: AutomationMode, repeat: Boolean) {
        this.mode = mode
        this.repeat = repeat
        snapshot = AutomationSnapshot(TaskPhase.WAITING_HOME)
        deadlineStarted = false
    }

    fun tick(screen: ScreenKind, openTask: TaskKey?, schedules: List<TaskSchedule>, nowMillis: Long): DirectorCommand {
        if (snapshot.phase in setOf(TaskPhase.PARKED, TaskPhase.STOPPED, TaskPhase.IDLE)) return DirectorCommand(DirectorOperation.PARK)
        if (snapshot.phase == TaskPhase.COMPLETE) return DirectorCommand(DirectorOperation.COMPLETE)
        val task = snapshot.task
        if (snapshot.phase == TaskPhase.WORKING) return DirectorCommand(DirectorOperation.WAIT, task)
        if (snapshot.phase == TaskPhase.NAVIGATING) {
            if (screen != ScreenKind.UNKNOWN && openTask == task) {
                snapshot = snapshot.copy(phase = TaskPhase.WORKING, reason = "TASK_RECOGNIZED")
                return DirectorCommand(DirectorOperation.WORK, task)
            }
            if (nowMillis >= deadline) return park("NAVIGATION_TIMEOUT")
            return DirectorCommand(DirectorOperation.WAIT, task)
        }
        if (snapshot.phase == TaskPhase.RETURNING_HOME) {
            if (screen != ScreenKind.CLEAN_HOME) {
                if (nowMillis >= deadline) return park("HOME_RETURN_TIMEOUT")
                return DirectorCommand(DirectorOperation.WAIT, task)
            }
            snapshot = snapshot.copy(phase = TaskPhase.WAITING_HOME, task = null, reason = "HOME_VERIFIED")
            deadlineStarted = false
        }
        if (mode == AutomationMode.SEMI_AUTO) {
            val available = schedules.firstOrNull { it.enabled && it.key == openTask && it.remainingBudget > 0 && it.dueAtMillis <= nowMillis }
            if (screen == ScreenKind.UNKNOWN || available == null) return DirectorCommand(DirectorOperation.WAIT)
            snapshot = snapshot.copy(phase = TaskPhase.WORKING, task = available.key, reason = "MANUAL_TASK_RECOGNIZED")
            return DirectorCommand(DirectorOperation.WORK, available.key)
        }
        if (screen != ScreenKind.CLEAN_HOME) {
            if (!deadlineStarted) { deadline = nowMillis + navigationTimeoutMillis; deadlineStarted = true }
            if (nowMillis >= deadline) return park("CLEAN_HOME_REQUIRED")
            return DirectorCommand(DirectorOperation.WAIT)
        }
        deadlineStarted = false
        val next = schedules.firstOrNull {
            it.enabled && it.remainingBudget > 0 && it.dueAtMillis <= nowMillis && (repeat || it.key !in snapshot.completed)
        }
        if (next == null) {
            snapshot = snapshot.copy(phase = if (repeat) TaskPhase.WAITING_DUE else TaskPhase.COMPLETE, reason = "NO_TASK_DUE")
            return DirectorCommand(if (repeat) DirectorOperation.WAIT else DirectorOperation.COMPLETE)
        }
        snapshot = snapshot.copy(phase = TaskPhase.NAVIGATING, task = next.key, reason = "TASK_SELECTED")
        deadline = nowMillis + navigationTimeoutMillis
        return DirectorCommand(DirectorOperation.NAVIGATE, next.key)
    }

    fun finish(task: TaskKey, outcome: TaskOutcome, nowMillis: Long): DirectorCommand {
        if (snapshot.phase != TaskPhase.WORKING || snapshot.task != task) return park("UNEXPECTED_COMPLETION")
        if (outcome !in setOf(TaskOutcome.COMPLETE, TaskOutcome.NOTHING_DUE)) return park(outcome.name)
        snapshot = snapshot.copy(completed = snapshot.completed + task)
        if (mode == AutomationMode.SEMI_AUTO) {
            snapshot = snapshot.copy(phase = TaskPhase.COMPLETE, reason = outcome.name)
            return DirectorCommand(DirectorOperation.COMPLETE, task)
        }
        snapshot = snapshot.copy(phase = TaskPhase.RETURNING_HOME, reason = outcome.name)
        deadline = nowMillis + navigationTimeoutMillis
        return DirectorCommand(DirectorOperation.RETURN_HOME, task)
    }

    fun stop() { snapshot = snapshot.copy(phase = TaskPhase.STOPPED, reason = "USER_STOP") }
    private fun park(reason: String): DirectorCommand {
        snapshot = snapshot.copy(phase = TaskPhase.PARKED, reason = reason)
        return DirectorCommand(DirectorOperation.PARK, snapshot.task)
    }
}
