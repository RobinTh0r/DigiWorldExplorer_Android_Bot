package de.robinthor.digiworldexplorer.strategy

data class DwsNavigationSettings(
    val allowLeft: Boolean = true,
    val forceForwardAttack: Boolean = false,
    val dashSpamUntilZero: Boolean = false,
    val collectOnlyEnergy: Boolean = false,
    val betterEnergyCollect: Boolean = true,
    val blindStageFailedTap: Boolean = false,
    val trackAllSprites: Boolean = true,
    val phoneSafeMovement: Boolean = true,
)

enum class DwsNavigationProfile {
    V3_CLASSIC,
    V4_DASH,
    V5_ALL_SPRITES;

    fun settings() = when (this) {
        V3_CLASSIC -> DwsNavigationSettings(
            betterEnergyCollect = false,
            blindStageFailedTap = true,
            trackAllSprites = false,
            phoneSafeMovement = false,
        )
        V4_DASH -> DwsNavigationSettings(
            allowLeft = false,
            forceForwardAttack = true,
            dashSpamUntilZero = true,
            collectOnlyEnergy = true,
            betterEnergyCollect = true,
            blindStageFailedTap = true,
            trackAllSprites = false,
            phoneSafeMovement = false,
        )
        V5_ALL_SPRITES -> DwsNavigationSettings(
            allowLeft = false,
            forceForwardAttack = true,
            dashSpamUntilZero = true,
            collectOnlyEnergy = true,
            betterEnergyCollect = true,
            blindStageFailedTap = true,
            trackAllSprites = true,
            phoneSafeMovement = true,
        )
    }

    companion object {
        fun fromPreference(value: String?): DwsNavigationProfile =
            entries.firstOrNull { it.name == value } ?: V3_CLASSIC
    }
}
