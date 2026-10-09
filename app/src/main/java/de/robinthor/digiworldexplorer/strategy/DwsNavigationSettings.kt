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
    /** Restores v4.0.0 classification and movement rules; updated game UI is observed jointly. */
    val legacyV4Core: Boolean = false,
)

enum class DwsNavigationProfile {
    V3_CLASSIC,
    V4_DASH,
    V5_ALL_SPRITES;

    fun availableFor(supporterUnlocked: Boolean): Boolean =
        supporterUnlocked || this != V5_ALL_SPRITES

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
            legacyV4Core = true,
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
            entries.firstOrNull { it.name == value } ?: V4_DASH

        fun fromPreferenceForAccess(value: String?, supporterUnlocked: Boolean): DwsNavigationProfile =
            fromPreference(value).takeIf { it.availableFor(supporterUnlocked) } ?: V4_DASH
    }
}
