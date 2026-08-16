package de.robinthor.digiworldexplorer.strategy

data class DwsNavigationSettings(
    val allowLeft: Boolean = true,
    val forceForwardAttack: Boolean = false,
    val dashSpamUntilZero: Boolean = false,
    val collectOnlyEnergy: Boolean = false,
    val betterEnergyCollect: Boolean = true,
    val blindStageFailedTap: Boolean = false,
)
