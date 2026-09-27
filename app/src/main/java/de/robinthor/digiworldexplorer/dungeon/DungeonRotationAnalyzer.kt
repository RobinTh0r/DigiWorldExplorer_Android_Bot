package de.robinthor.digiworldexplorer.dungeon

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import android.util.Log
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.automation.HomeScreenDetector
import de.robinthor.digiworldexplorer.automation.ObservedScreen
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.vision.*

/** Ticket grant, battle result and return are separate transitions. */
object DungeonRotationAnalyzer {
    private var controller: DungeonRotationController? = null
    private var settings: DungeonRotationSettings? = null
    private var activeKey: DungeonKey? = null
    private var waiting = ""
    private var waitingAt = 0L
    private var settleUntil = 0L
    private var candidate = ""
    private var matches = 0
    private var returning = false
    private var returningHome = false
    private var unknownAt = 0L
    private var startRetries = 0
    private var retryAt = 0L
    private var rewardFallbackAt = 0L
    private var waitingSawTransition = false
    private var passUsage = DungeonPassUsage()

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        if (!AutomationState.enabled || !DungeonRotationRequest.active()) return false
        val service = DigiWorldAccessibilityService.instance ?: return true
        if (de.robinthor.digiworldexplorer.license.SupporterLicenseManager.load(service) == null) {
            park(service, "Beta access required")
            return true
        }
        val plane = image.planes.firstOrNull() ?: return true
        val w = minOf(width,image.width); val h = minOf(height,image.height)
        val bytes = plane.buffer
        if (plane.pixelStride < 3 || w <= 0 || h <= 0 ||
            (h-1L)*plane.rowStride+(w-1L)*plane.pixelStride+2 >= bytes.limit()) return true
        val frame = PixelFrame(w,h) { x,y ->
            val i = y*plane.rowStride+x*plane.pixelStride
            Color.rgb(bytes.get(i).toInt() and 255,bytes.get(i+1).toInt() and 255,bytes.get(i+2).toInt() and 255)
        }
        val v = GameViewport.fit(w,h)
        val now = SystemClock.elapsedRealtime()
        if(now < settleUntil) return true
        val cfg = settings ?: DungeonSettingsStore.load(service).also { settings=it }
        val key = activeKey
        // Failure recovery belongs to this run; unrelated entry analyzers stay excluded.
        if (key == DungeonKey.NETWORK_DEFENSE && waiting == "battle" &&
            de.robinthor.digiworldexplorer.network.NetworkDefenseFrameAnalyzer.analyze(image, w, h, rotationOwned = true)) return true
        if (key != null && key != DungeonKey.NETWORK_DEFENSE && waiting == "battle" &&
            de.robinthor.digiworldexplorer.feed.StageFailedFrameAnalyzer.detect(w, h, frame::argbAt)) {
            onLoss()
            tap(service, v, NormalizedPoint(.5, .84), now, "Closing dungeon loss")
            return true
        }
        if (key == DungeonKey.APOCALYMON_WALL && apocalymonResultVisible(frame, v)) {
            // Apocalymon has its own tall "Ergebnis" sheet which does not match the generic
            // dungeon reward template. Its panel can also resemble the challenge dialog, so this
            // check must run before returned-panel recovery.
            if (!DungeonDailyStore.snapshot(service).isComplete(key)) confirmBattle(service, key, won = true)
            waiting=""; waitingSawTransition=false; startRetries=0; retryAt=0; unknownAt=0
            controller?.finishCurrent(); returning=true
            tap(service,v,NormalizedPoint(.50,.793),now,"Apocalymon result: closing and locking until 08:00")
            return true
        }
        if(key == null) {
            val orphanPanel = DungeonKey.entries.firstNotNullOfOrNull { DungeonPanelDetector.detect(frame,it,v) }
            if(orphanPanel != null) {
                unknownAt=0L; settleUntil=now+1_300L; candidate=""; matches=0
                status(service,"Closing open dungeon window to resume")
                Log.i("DigiWorldDungeonRotation","RESUME closing orphan panel ${orphanPanel.kind}")
                service.dispatchBack { ok -> if(!ok) park(service,"Could not close open dungeon window") }
                return true
            }
        }
        val rewardVisible = key != null && DungeonScreenDetector.detect(w,h,frame::argbAt).screen == DungeonScreen.REWARD
        if(rewardVisible && DungeonFrameAnalyzer.analyze(image,w,h,resultsOnly=true)) {
            unknownAt=0L
            if(key == DungeonKey.DAILY) DungeonDailyStore.markComplete(service, DungeonKey.DAILY)
            if(rewardFallbackAt == 0L) rewardFallbackAt = now
            // The VS Destroy reward uses a longer reveal variant which can outlive the generic
            // Tower handler. Retry the explicit close prompt if the generic result handler was
            // swallowed; four seconds still leaves ample time for the reveal animation.
            if(now-rewardFallbackAt >= 4_000L) {
                rewardFallbackAt=0L
                tap(service,v,NormalizedPoint(.5,.825),now,"Closing daily reward")
            }
            return true
        }
        rewardFallbackAt=0L
        if(waiting.isNotEmpty()) {
            val retryPanel = if(key != null) DungeonPanelDetector.detect(frame,key,v) else null
            val returnedToPanel = retryPanel?.kind in setOf("challenge", "network_challenge", "network_matching", "destroy", "ad")
            if(retryPanel == null) waitingSawTransition = true
            val returnDelay = if(waiting == "ad") 2_500L else 15_000L
            if(returnedToPanel && now-waitingAt > returnDelay && (waiting != "ad" || waitingSawTransition)) {
                val completed = waiting
                waiting=""; startRetries=0; retryAt=0; unknownAt=0; waitingSawTransition=false
                if(completed=="battle") confirmBattle(service, key!!, won = true)
                if(completed=="ad") {
                    passUsage.reserveAd(key!!)
                    DungeonDailyStore.record(service,key) { it.copy(ads=it.ads+1) }
                }
                Log.i("DigiWorldDungeonRotation","RESUME $key $completed confirmed by returned panel ${retryPanel!!.kind}:${retryPanel.remaining}")
                status(service,"${key!!.name}: ${if(completed=="ad") "Ad ticket received" else "battle complete"}")
                candidate=""; matches=0
                return true
            }
            if(waiting == "ad" && returnedToPanel && !waitingSawTransition &&
                now-waitingAt > 1_500L && now-retryAt > 1_500L && startRetries < 2) {
                startRetries++; retryAt=now
                tap(service,v,retryPanel!!.target,now,"Retrying unaccepted Ad-Skip ($startRetries)")
                return true
            }
            if(waiting == "battle" && now-waitingAt > 4_000L && now-retryAt > 4_000L &&
                retryPanel?.kind in setOf("challenge", "network_challenge", "network_matching", "destroy") && retryPanel?.remaining == 1 && startRetries < 3) {
                startRetries++; retryAt=now
                tap(service,v,if(key==DungeonKey.NETWORK_DEFENSE) NormalizedPoint(.5,.79) else retryPanel!!.target,now,"Retrying unaccepted Start ($startRetries)")
                return true
            }
            status(service, if(waiting == "ad") "Ad-Skip: waiting for ticket" else "${key?.name}: battle / matching")
            if(now-waitingAt > if(waiting == "ad") 30_000L else 240_000L)
                park(service,"${key?.name}: result not confirmed")
            return true
        }
        val panel = if(key != null) DungeonPanelDetector.detect(frame,key,v) else null
        if(panel != null) {
            unknownAt=0L
            if(!stable("${key}:${panel.kind}:${panel.remaining}")) return true
            if(panel.kind == "network_leave") {
                tap(service,v,panel.target,now,"Leaving network team")
                return true
            }
            if(returning) {
                tap(service,v,NormalizedPoint(.94,.87),now,"Closing ${key!!.name}")
                return true
            }
            val used = DungeonDailyStore.snapshot(service).progress[key] ?: DungeonDailyProgress()
            when(panel.kind) {
                "network_confirm" -> tap(service,v,panel.target,now,"Confirming team notice")
                "ad" -> {
                    if(!AutomationState.adSkipPassEnabled || !cfg.useAdAttempts || panel.remaining == 0 || used.ads >= 2) {
                        finishCard(service,v,now); return true
                    }
                    if(panel.remaining == null) { park(service,"Ad counter unreadable: ${key!!.name}"); return true }
                    waiting="ad"; waitingAt=now; waitingSawTransition=false; startRetries=0; retryAt=now
                    tap(service,v,panel.target,now,"${key!!.name}: Ad-Skip ${used.ads+1}/2")
                }
                "destroy" -> {
                    // VS Battles is a once-daily destruction/claim action. The displayed ticket
                    // counter can be zero, so it must not be treated like a normal challenge.
                    if(passUsage.attempts(key!!) >= 1) { finishCard(service,v,now); return true }
                    waiting="battle"; waitingAt=now
                    startRetries=0; retryAt=now
                    passUsage.reserveAttempt(key)
                    tap(service,v,panel.target,now,"${key.name}: daily Destroy")
                }
                "challenge", "network_challenge", "network_matching" -> {
                    val configuredLimit = attemptLimit(key!!, cfg.normalAttempts)
                    val limit = if (key in setOf(DungeonKey.DEMIDEVIMON, DungeonKey.BAKEMON))
                        configuredLimit else passUsage.limit(key!!, configuredLimit)
                    val spent = passUsage.attempts(key)
                    // Apocalymon's decorative/list-style counter is consistently misread as zero
                    // on the live dialog. It is a once-daily action, so its verified Challenge
                    // button is authoritative; the normal ticket counter remains authoritative
                    // for every repeatable dungeon.
                    val noTickets = panel.remaining == 0 && key != DungeonKey.APOCALYMON_WALL
                    if(noTickets || spent >= limit) { finishCard(service,v,now); return true }
                    if(panel.remaining == null) { park(service,"Ticket counter unreadable: ${key!!.name}"); return true }
                    val target = if(key==DungeonKey.NETWORK_DEFENSE) NormalizedPoint(.5,.79) else panel.target
                    waiting="battle"; waitingAt=now
                    startRetries=0; retryAt=now
                    // Daily actions are persisted only after visual proof of a battle/result. This
                    // keeps an unaccepted Start tap from suppressing Apocalymon for the entire day.
                    if (key !in DungeonPassPolicy.dailyLimited)
                        DungeonDailyStore.record(service,key) { it.copy(attempts=it.attempts+1) }
                    passUsage.reserveAttempt(key)
                    tap(service,v,target,now,"${key.name}: starting battle / Matching")
                }
            }
            return true
        }
        // Modal overlays dim the page header. Background cards never authorize navigation.
        val headerY = if (v.usesTallPhoneLayout) .128 else .095
        val header = frame.ratioInViewportPatch(v,NormalizedPoint(.65,headerY),.10,.012) {
            val hsv=it.hsv(); hsv.hue in 90..115 && hsv.value >= 180 && hsv.saturation >= 90
        }
        val reading = if(header > .5) DungeonListDetector.detect(frame,v) else DungeonListReading(DungeonListPosition.NONE,emptyList())
        if(reading.position != DungeonListPosition.NONE) {
            unknownAt=0L
            if(!stable("list:${reading.position}")) return true
            DungeonRotationRequest.listVerified()
            val daily = DungeonDailyStore.snapshot(service)
            val scheduler = controller ?: DungeonRotationController(cfg.enabledCards,DungeonPassPolicy.locked(daily)).also { controller=it }
            if(returning) { activeKey=null; returning=false }
            else if(activeKey != null) { scheduler.releaseCurrent(); activeKey=null }
            val decision = scheduler.onList(reading) { cfg.budget(it,AutomationState.adSkipPassEnabled).adTickets > 0 }
            (scheduler.completed()-daily.completed).filter { it !in DungeonPassPolicy.dailyLimited }
                .forEach { DungeonDailyStore.markComplete(service,it) }
            Log.i("DigiWorldDungeonRotation","list=${reading.position} decision=$decision")
            when(decision.command) {
                DungeonRotationCommand.SELECT_CARD -> {
                    activeKey=decision.card!!.key
                    tap(service,v,decision.card.center,now,"Opening ${activeKey!!.name}")
                }
                DungeonRotationCommand.SWIPE_TOP, DungeonRotationCommand.SWIPE_BOTTOM -> {
                    val top=decision.command==DungeonRotationCommand.SWIPE_TOP
                    settleUntil=now+1_500; matches=0
                    val x=v.left+v.width*.5f; val near=v.top+v.height*.30f; val far=v.top+v.height*.86f
                    status(service,"Scrolling ${if(top) "up" else "down"}")
                    service.dispatchValidatedSwipe(x,if(top) near else far,x,if(top) far else near) { ok -> if(!ok) park(service,"Scroll failed") }
                }
                DungeonRotationCommand.COMPLETE -> {
                    returningHome=true
                    tap(service,v,NormalizedPoint(.50,.955),now,"Rotation complete: returning Home")
                }
                DungeonRotationCommand.PARK -> park(service,decision.reason)
                else -> Unit
            }
            return true
        }
        if(activeKey == null && HomeScreenDetector.detect(w,h,frame::argbAt)) {
            unknownAt=0L
            if(!stable("home")) return true
            val homeViewport = HomeScreenDetector.viewport(w, h, frame::argbAt) ?: v
            if(returningHome) {
                DungeonRotationRequest.complete()
                // The passive classifier may already have promoted the visible screen to HOME.
                // A late DUNGEON-sourced status is then correctly rejected by ScreenDirector,
                // which used to leave an older message such as "Detection reloaded" visible.
                // Publish completion independently of the previous screen owner and release the
                // standalone dungeon session so the overlay returns to normal Home observation.
                DungeonFrameAnalyzer.reset()
                service.showStatusOnly("Dungeon rotation complete — Home")
                Log.i("DigiWorldDungeonRotation","COMPLETE verified Home")
                reset()
            } else {
                DungeonRotationRequest.openingList()
                tap(service,homeViewport,NormalizedPoint(.375,.955),now,"Opening dungeon list")
            }
            return true
        }
        if(unknownAt==0L) unknownAt=now
        status(service,"Dungeon: waiting for stable screen")
        if(now-unknownAt > 20_000L) park(service,"Dungeon screen not confirmed")
        return true
    }

    private fun stable(value: String): Boolean {
        if(candidate==value) matches++ else { candidate=value; matches=1 }
        return matches>=3
    }
    private fun status(service: DigiWorldAccessibilityService,label: String) = service.showStatusOnly(label,sourceScreen=ObservedScreen.DUNGEON)
    private fun park(service: DigiWorldAccessibilityService,reason: String) {
        DungeonRotationRequest.park(reason); status(service,reason)
        Log.w("DigiWorldDungeonRotation","PARK $reason")
    }
    private fun tap(service: DigiWorldAccessibilityService,v: GameViewport,p: NormalizedPoint,now: Long,label: String) {
        settleUntil=now+1_300; matches=0
        status(service,label)
        Log.i("DigiWorldDungeonRotation","$label target=$p waiting=$waiting")
        val (x,y)=v.pixel(p)
        service.dispatchValidatedTap(x.toFloat(),y.toFloat()) { ok -> if(!ok) park(service,"Tap failed: $label") }
    }
    private fun finishCard(service: DigiWorldAccessibilityService,v: GameViewport,now: Long) {
        val key=activeKey ?: return
        if (key !in DungeonPassPolicy.dailyLimited)
            DungeonDailyStore.markComplete(service,key)
        controller?.finishCurrent()
        returning=true
        tap(service,v,NormalizedPoint(.94,.87),now,"${key.name}: done, returning to list")
    }
    fun onReward() {
        val key=activeKey ?: return
        if(waiting.isEmpty() || SystemClock.elapsedRealtime()-waitingAt < 1_500) return
        if(waiting=="ad") {
            // The reward overlay is the visual proof that the Ad-Skip tap was accepted. Keep the
            // transaction open until the panel returns, then persist the consumed Ad exactly once.
            waitingSawTransition=true
            return
        }
        Log.i("DigiWorldDungeonRotation","RESULT $key $waiting")
        if(waiting=="battle") DigiWorldAccessibilityService.instance?.let { service -> confirmBattle(service, key, won = true) }
        waiting=""
    }
    fun onLoss() {
        val key=activeKey ?: return
        if(waiting!="battle") return
        DigiWorldAccessibilityService.instance?.let { service -> confirmBattle(service, key, won = false) }
        waiting=""
    }
    private fun confirmBattle(service: DigiWorldAccessibilityService, key: DungeonKey, won: Boolean) {
        DungeonDailyStore.record(service,key) {
            it.copy(
                attempts = if (key in DungeonPassPolicy.dailyLimited) it.attempts + 1 else it.attempts,
                wins = it.wins + if (won) 1 else 0,
                losses = it.losses + if (won) 0 else 1
            )
        }
        if (key in DungeonPassPolicy.dailyLimited) DungeonDailyStore.markComplete(service,key)
    }
    internal fun panelKind(frame: PixelFrame,viewport: GameViewport,key: DungeonKey) = DungeonPanelDetector.detect(frame,key,viewport)?.kind ?: ""
    internal fun attemptLimit(key: DungeonKey, configured: Int) =
        if (key in setOf(DungeonKey.DEMIDEVIMON, DungeonKey.BAKEMON)) 15 else configured
    private fun apocalymonResultVisible(frame: PixelFrame, viewport: GameViewport): Boolean {
        fun blue(rgb: Rgb): Boolean {
            val hsv = rgb.hsv()
            return hsv.hue in 88..112 && hsv.saturation >= 90 && hsv.value >= 75
        }
        val title = frame.ratioInViewportPatch(viewport, NormalizedPoint(.50,.16), .25,.035, predicate=::blue)
        val body = frame.ratioInViewportPatch(viewport, NormalizedPoint(.50,.48), .30,.20, predicate=::blue)
        val close = frame.ratioInViewportPatch(viewport, NormalizedPoint(.50,.793), .13,.025, predicate=::blue)
        return title >= .30 && body >= .55 && close >= .45
    }
    fun reset() {
        controller=null; settings=null; activeKey=null; waiting=""; waitingAt=0; settleUntil=0
        candidate=""; matches=0; returning=false; returningHome=false; unknownAt=0
        startRetries=0; retryAt=0; waitingSawTransition=false
        rewardFallbackAt=0L
        passUsage= DungeonPassUsage()
    }
}
