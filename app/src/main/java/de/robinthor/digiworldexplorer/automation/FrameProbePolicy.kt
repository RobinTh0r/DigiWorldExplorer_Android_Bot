package de.robinthor.digiworldexplorer.automation

/** Pure ownership rules shared by capture orchestration and regression tests. */
object FrameProbePolicy {
    /**
     * A started Network Defense run owns every intermediate/loading frame until it releases the
     * session. Generic title/login detection must not tap through the battle or an opened card.
     */
    fun allowGenericGameEntry(
        featureFrame: Boolean,
        networkDefenseSessionActive: Boolean,
        worldSearchCalibrated: Boolean = false,
        worldSearchSessionActive: Boolean = false,
        digiCopilotOwns: Boolean,
        awaitingFarm: Boolean,
        rewardSequenceActive: Boolean,
    ): Boolean = featureFrame &&
        !networkDefenseSessionActive &&
        !worldSearchCalibrated &&
        !worldSearchSessionActive &&
        (!digiCopilotOwns || !awaitingFarm) &&
        !rewardSequenceActive
}
