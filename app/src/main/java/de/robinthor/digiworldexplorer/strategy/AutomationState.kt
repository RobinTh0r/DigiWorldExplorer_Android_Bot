package de.robinthor.digiworldexplorer.strategy

object AutomationState {
 @Volatile var enabled=false
 @Volatile var overlayEnabled=true
 @Volatile var autoPurchaseEnabled=true
 @Volatile var autoDungeonEnabled=true
 @Volatile var autoNetworkDefenseEnabled=false
 @Volatile var autoFeedEnabled=false
 @Volatile var forceLegacyCaptureMetrics=false
 @Volatile var summonTouchCorrection=false
 @Volatile var dwsNavigationSettings=DwsNavigationSettings()
 fun stop(){enabled=false;AutoMoveController.reset()}
}