package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class BondFarmCycleTest {
    @Test fun completedTourRequestsOneImmediateVisitAtSafeHome() {
        BondCycleTimer.resetForTest()
        val cycle = BondFarmCycle()
        cycle.tick(CycleScreen.HOME, false, false, 0)
        cycle.requestVisit()
        assertEquals(CycleStep.BOND, cycle.tick(CycleScreen.OTHER, false, false, 1))
        assertEquals(CycleStep.BOND, cycle.tick(CycleScreen.HOME, false, true, 2))
        assertEquals(CycleStep.OPEN_EXPLORE, cycle.tick(CycleScreen.HOME, false, false, 3))
        cycle.tick(CycleScreen.EXPLORE, false, false, 4)
        cycle.tick(CycleScreen.FIELD, false, false, 5)
        cycle.tick(CycleScreen.FIELD, true, false, 6)
        cycle.tick(CycleScreen.EXPLORE, true, false, 7)
        assertEquals(CycleStep.BOND, cycle.tick(CycleScreen.HOME, false, false, 8))
        assertEquals(true, cycle.consumeReturnedHome())
        assertEquals(CycleStep.BOND, cycle.tick(CycleScreen.HOME, false, false, 9))
    }

    @Test fun farmVisitWaitsForBondAndReturnsBeforeRestartingClock() {
        BondCycleTimer.resetForTest()
        val cycle = BondFarmCycle()
        assertEquals(CycleStep.BOND, cycle.tick(CycleScreen.HOME, false, false, 0))
        cycle.requestVisit()
        assertEquals(CycleStep.BOND, cycle.tick(CycleScreen.HOME, false, true, 1))
        assertEquals(CycleStep.OPEN_EXPLORE, cycle.tick(CycleScreen.HOME, false, false, 2))
        assertEquals(CycleStep.WAIT, cycle.tick(CycleScreen.HOME, false, false, 3))
        assertEquals(CycleStep.OPEN_FIELD, cycle.tick(CycleScreen.EXPLORE, false, false, 601_000))
        assertEquals(CycleStep.FARM, cycle.tick(CycleScreen.FIELD, false, false, 602_000))
        assertEquals(CycleStep.CLOSE_FIELD, cycle.tick(CycleScreen.FIELD, true, false, 603_000))
        assertEquals(CycleStep.HOME, cycle.tick(CycleScreen.EXPLORE, true, false, 604_000))
        assertEquals(CycleStep.BOND, cycle.tick(CycleScreen.HOME, false, false, 605_000))
        assertEquals(true, cycle.consumeReturnedHome())
    }

    @Test fun unknownScreenNeverStartsNavigationAndMissingDestinationParks() {
        val cycle = BondFarmCycle()
        cycle.tick(CycleScreen.HOME, false, false, 0)
        cycle.requestVisit()
        assertEquals(CycleStep.BOND, cycle.tick(CycleScreen.OTHER, false, false, 11))
        assertEquals(CycleStep.OPEN_EXPLORE, cycle.tick(CycleScreen.HOME, false, false, 12))
        assertEquals(CycleStep.PARK, cycle.tick(CycleScreen.OTHER, false, false, 30_012))
    }

    @Test fun globalCooldownStartsOnlyAfterFarmReturnedHome() {
        BondCycleTimer.resetForTest()
        BondCycleTimer.bondCompleted()
        assertEquals(false, BondCycleTimer.canStartBond(1))
        BondCycleTimer.farmReturnedHome(100)
        assertEquals(1_200_000L, BondCycleTimer.remainingMillis(100))
        assertEquals(false, BondCycleTimer.canStartBond(1_200_099))
        assertEquals(true, BondCycleTimer.canStartBond(1_200_100))
    }
}
