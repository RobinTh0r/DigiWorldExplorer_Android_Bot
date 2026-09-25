package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.*
import org.junit.Test

class TaskDirectorTest {
    private val tasks = listOf(TaskSchedule(TaskKey.FARM, true), TaskSchedule(TaskKey.BOND_TOUR, true))

    @Test fun `expedition verifies home between two tasks`() {
        val director = TaskDirector()
        director.start(AutomationMode.FULL_AUTOPILOT, false)
        assertEquals(DirectorOperation.WAIT, director.tick(ScreenKind.UNKNOWN, null, tasks, 0).operation)
        assertEquals(DirectorCommand(DirectorOperation.NAVIGATE, TaskKey.FARM), director.tick(ScreenKind.CLEAN_HOME, null, tasks, 1))
        assertEquals(DirectorOperation.WORK, director.tick(ScreenKind.FARM, TaskKey.FARM, tasks, 2).operation)
        assertEquals(DirectorOperation.RETURN_HOME, director.finish(TaskKey.FARM, TaskOutcome.COMPLETE, 3).operation)
        assertEquals(DirectorOperation.WAIT, director.tick(ScreenKind.FARM, TaskKey.FARM, tasks, 4).operation)
        assertEquals(DirectorCommand(DirectorOperation.NAVIGATE, TaskKey.BOND_TOUR), director.tick(ScreenKind.CLEAN_HOME, null, tasks, 5))
    }

    @Test fun `co pilot never emits navigation or home return`() {
        val director = TaskDirector()
        director.start(AutomationMode.SEMI_AUTO, true)
        assertEquals(DirectorOperation.WAIT, director.tick(ScreenKind.CLEAN_HOME, null, tasks, 0).operation)
        assertEquals(DirectorOperation.WORK, director.tick(ScreenKind.FARM, TaskKey.FARM, tasks, 1).operation)
        assertEquals(DirectorOperation.COMPLETE, director.finish(TaskKey.FARM, TaskOutcome.COMPLETE, 2).operation)
    }

    @Test fun `unknown destination times out and stop never starts next task`() {
        val director = TaskDirector(100)
        director.start(AutomationMode.FULL_AUTOPILOT, false)
        director.tick(ScreenKind.CLEAN_HOME, null, tasks, 0)
        assertEquals(DirectorOperation.PARK, director.tick(ScreenKind.UNKNOWN, null, tasks, 100).operation)
        director.stop()
        assertEquals(DirectorOperation.PARK, director.tick(ScreenKind.CLEAN_HOME, null, tasks, 101).operation)
    }

    @Test fun `repeat uses fresh deadlines and budgets and does not busy loop completed work`() {
        val director = TaskDirector()
        director.start(AutomationMode.FULL_AUTOPILOT, true)
        val later = listOf(TaskSchedule(TaskKey.FARM, true, dueAtMillis = 100))
        assertEquals(DirectorOperation.WAIT, director.tick(ScreenKind.CLEAN_HOME, null, later, 0).operation)
        assertEquals(DirectorOperation.NAVIGATE, director.tick(ScreenKind.CLEAN_HOME, null, later, 100).operation)
    }
}
