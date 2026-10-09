package de.robinthor.digiworldexplorer.dungeon

/** An Ad-Skip starts only from a zero-ticket panel. A positive challenge is grant proof,
 * never permission to retry the ad by pressing a different button. */
internal object DungeonAdReturnPolicy {
    fun ticketGranted(kind: String?, remaining: Int?) =
        kind in setOf("challenge", "network_challenge", "network_matching") && remaining == 1

    fun canRetry(kind: String?, remaining: Int?, sawTransition: Boolean) =
        kind == "ad" && remaining == 1 && !sawTransition
}
