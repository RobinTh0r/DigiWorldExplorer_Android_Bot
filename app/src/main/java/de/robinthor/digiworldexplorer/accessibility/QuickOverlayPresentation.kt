package de.robinthor.digiworldexplorer.accessibility

/** Temporary task visibility must not overwrite the user's idle overlay preference. */
internal fun showDirectorCard(preferenceEnabled:Boolean,dungeonActive:Boolean,copilotActive:Boolean)=
    preferenceEnabled && !dungeonActive && !copilotActive
