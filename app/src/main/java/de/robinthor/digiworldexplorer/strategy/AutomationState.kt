package de.robinthor.digiworldexplorer.strategy

import de.robinthor.digiworldexplorer.automation.AutomationMode

object AutomationState {
 @Volatile var enabled=false
 @Volatile var overlayEnabled=true
 @Volatile var autoPurchaseEnabled=true
 @Volatile var autoDungeonEnabled=true
 @Volatile var autoNetworkDefenseEnabled=false
 @Volatile var autoFeedEnabled=false
 @Volatile var autoRunnerEnabled=false
 @Volatile var autoFarmEnabled=false
 @Volatile var farmWateringEnabled=true
 @Volatile var adSkipPassEnabled=false
 @Volatile var mode=AutomationMode.SEMI_AUTO
 @Volatile var forceLegacyCaptureMetrics=false
 @Volatile var summonTouchCorrection=false
 @Volatile var dwsNavigationSettings=DwsNavigationSettings()
 fun stop(){enabled=false;AutoMoveController.reset();de.robinthor.digiworldexplorer.farm.FarmHarvestAnalyzer.reset();de.robinthor.digiworldexplorer.dungeon.DungeonRotationRequest.cancel();de.robinthor.digiworldexplorer.dungeon.DungeonRotationAnalyzer.reset()}
}
