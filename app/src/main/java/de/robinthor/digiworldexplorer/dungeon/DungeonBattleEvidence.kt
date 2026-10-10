package de.robinthor.digiworldexplorer.dungeon

/** Returning to an entry panel is not a victory. Result accounting is once per start;
 * repeated loss-dialog frames may be dismissed again but cannot spend another attempt. */
class DungeonBattleEvidence {
    private var started=false
    private var outcome:BattleOutcome?=null
    private var returnedUnconfirmed=false
    var consecutiveUnconfirmed=0
        private set
    var consecutiveLosses=0
        private set
    fun begin() { started=true;outcome=null;returnedUnconfirmed=false }
    fun returnedWithoutResult():Boolean {
        if(!started || outcome!=null || returnedUnconfirmed)return false
        returnedUnconfirmed=true;consecutiveUnconfirmed++
        return true
    }
    fun result(value:BattleOutcome):Boolean {
        if(!started || outcome!=null)return false
        if(returnedUnconfirmed) { consecutiveUnconfirmed--;returnedUnconfirmed=false }
        outcome=value
        when(value) {
            BattleOutcome.LOSS -> consecutiveLosses++
            BattleOutcome.WIN -> { consecutiveLosses=0;consecutiveUnconfirmed=0 }
            BattleOutcome.UNKNOWN -> Unit
        }
        return true
    }
    fun canStart(lossLimit:Int)=consecutiveLosses<lossLimit.coerceAtLeast(1)
    fun canRetryStart(sawTransition:Boolean)=started && outcome==null && !sawTransition
}
