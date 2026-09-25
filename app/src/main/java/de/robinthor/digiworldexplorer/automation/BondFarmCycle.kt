package de.robinthor.digiworldexplorer.automation

enum class CycleScreen { HOME, EXPLORE, FIELD, OTHER }
enum class CycleStep { BOND, OPEN_EXPLORE, OPEN_FIELD, FARM, CLOSE_FIELD, HOME, WAIT, PARK }

/** Farm visits interrupt Bond only at a verified Home boundary. All clocks are monotonic. */
class BondFarmCycle {
    var phase = CycleStep.BOND
        private set
    private var deadline = 0L
    private var visitRequested = false
    private var returnedHome = false

    fun requestVisit() { visitRequested = true }
    fun consumeReturnedHome(): Boolean = returnedHome.also { returnedHome = false }

    fun tick(screen: CycleScreen, farmFinished: Boolean, bondBusy: Boolean, now: Long): CycleStep {
        if (phase == CycleStep.PARK) {
            if (screen == CycleScreen.FIELD && BondCycleTimer.awaitingFarm()) phase = CycleStep.FARM
            else return phase
        }
        if (phase == CycleStep.BOND) {
            if (screen == CycleScreen.FIELD && visitRequested) {
                visitRequested = false
                phase = CycleStep.FARM
            } else if (screen == CycleScreen.HOME && !bondBusy && visitRequested) {
                visitRequested = false
                phase = CycleStep.OPEN_EXPLORE
                deadline = now + 30_000
                return CycleStep.OPEN_EXPLORE
            } else return CycleStep.BOND
        }
        when (phase) {
            CycleStep.OPEN_EXPLORE -> if (screen == CycleScreen.EXPLORE) {
                phase = CycleStep.OPEN_FIELD
                deadline = now + 30_000
                return CycleStep.OPEN_FIELD
            }
            CycleStep.OPEN_FIELD -> if (screen == CycleScreen.FIELD) phase = CycleStep.FARM
            CycleStep.FARM -> {
                if (screen == CycleScreen.FIELD && farmFinished) {
                    phase = CycleStep.CLOSE_FIELD
                    deadline = now + 30_000
                    return CycleStep.CLOSE_FIELD
                }
                return CycleStep.FARM
            }
            CycleStep.CLOSE_FIELD -> if (screen == CycleScreen.EXPLORE) {
                phase = CycleStep.HOME
                deadline = now + 30_000
                return CycleStep.HOME
            }
            else -> Unit
        }
        if (phase in setOf(CycleStep.CLOSE_FIELD, CycleStep.HOME) && screen == CycleScreen.HOME) {
            phase = CycleStep.BOND
            returnedHome = true
            return CycleStep.BOND
        }
        if (phase == CycleStep.FARM) return CycleStep.FARM
        if (now >= deadline) { phase = CycleStep.PARK; return CycleStep.PARK }
        return CycleStep.WAIT
    }
}
