package de.robinthor.digiworldexplorer.strategy

object AutomationState {
 @Volatile var enabled=false
 @Volatile var overlayEnabled=true
 fun stop(){enabled=false;AutoMoveController.reset()}
}