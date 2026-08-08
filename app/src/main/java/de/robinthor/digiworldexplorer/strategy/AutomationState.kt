package de.robinthor.digiworldexplorer.strategy

object AutomationState {
 @Volatile var enabled=false
 @Volatile var overlayEnabled=true
 @Volatile var autoPurchaseEnabled=true
 @Volatile var autoDungeonEnabled=true
 @Volatile var autoNetworkDefenseEnabled=false
 @Volatile var forceLegacyCaptureMetrics=false
 fun stop(){enabled=false;AutoMoveController.reset()}
}