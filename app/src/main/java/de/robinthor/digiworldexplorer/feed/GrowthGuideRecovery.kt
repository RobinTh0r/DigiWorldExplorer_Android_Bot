package de.robinthor.digiworldexplorer.feed

/** First observation blocks competing input; two fresh observations allow dismissal. */
internal class GrowthGuideRecovery {
    @Volatile var visible=false;private set
    var confirmations=0;private set
    var attempts=0;private set
    private var lastObservation:Long?=null
    private var nextTap=0L
    @Synchronized fun observe(found:Boolean,now:Long) {
        if(lastObservation?.let {now-it !in 0..2_000}==true)reset()
        lastObservation=now
        if(!found){reset();return}
        visible=true;confirmations++
    }
    @Synchronized fun mayTap(now:Long)=visible && confirmations>=2 && attempts<3 && now>=nextTap
    @Synchronized fun dispatched(now:Long){attempts++;nextTap=now+1_500}
    @Synchronized fun rejected(){attempts=(attempts-1).coerceAtLeast(0)}
    @Synchronized fun reset(){visible=false;confirmations=0;attempts=0;lastObservation=null;nextTap=0L}
}
