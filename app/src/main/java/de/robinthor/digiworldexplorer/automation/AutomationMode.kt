package de.robinthor.digiworldexplorer.automation

/**
 * SEMI_AUTO only operates on a task screen which the user opened manually.
 * FULL_AUTOPILOT may navigate, but only after a positively recognized clean home screen.
 */
enum class AutomationMode {
    SEMI_AUTO,
    FULL_AUTOPILOT;

    companion object {
        fun fromPreference(value: String?): AutomationMode =
            entries.firstOrNull { it.name == value } ?: SEMI_AUTO
    }
}

enum class AutomationScreen {
    UNKNOWN,
    CLEAN_HOME,
    TASK_SCREEN,
    DIALOG,
}

enum class AutomationIntent {
    ANALYZE,
    ACT_IN_OPEN_TASK,
    NAVIGATE_FROM_HOME,
}

/** Pure policy gate shared by future task controllers. */
object AutomationModePolicy {
    fun allows(mode: AutomationMode, screen: AutomationScreen, intent: AutomationIntent): Boolean =
        when (intent) {
            AutomationIntent.ANALYZE -> true
            AutomationIntent.ACT_IN_OPEN_TASK -> screen == AutomationScreen.TASK_SCREEN
            AutomationIntent.NAVIGATE_FROM_HOME ->
                mode == AutomationMode.FULL_AUTOPILOT && screen == AutomationScreen.CLEAN_HOME
        }
}
