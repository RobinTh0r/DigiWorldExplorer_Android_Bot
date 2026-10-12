package de.robinthor.digiworldexplorer.diagnostics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import de.robinthor.digiworldexplorer.capture.AnalysisImage as Image
import android.os.Build
import android.os.SystemClock
import androidx.core.content.FileProvider
import de.robinthor.digiworldexplorer.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class DiagnosticSession(val directory: File, val name: String, val bytes: Long)

internal fun diagnosticSessionLabel(name: String): String = runCatching {
    val raw = name.removePrefix("diagnostic-")
    val parsed = LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    parsed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm:ss"))
}.getOrElse { name.removePrefix("diagnostic-") }

/** Opt-in, bounded diagnostics for physical-device runs without USB debugging. */
object PersistentDiagnosticLog {
    private const val PREF_ENABLED = "diagnostic_mode"
    private const val ROOT = "diagnostics"
    private const val MAX_SESSIONS = 6
    private const val MAX_SCREENSHOTS = 50
    private const val SCREENSHOT_COOLDOWN = 4_000L
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault())
    @Volatile private var appContext: Context? = null
    @Volatile private var enabled = false
    private var sessionDir: File? = null
    private var pendingScreenshot: String? = null
    private data class ActionShot(val reason: String, val dueAt: Long,val x:Int,val y:Int,val generation:Long?)
    private val actionShots = ArrayDeque<ActionShot>()
    private var lastActionSampleAt = 0L
    private var actionSequence = 0
    private var lastScreenshotAt = 0L
    private var lastArea = ""
    private var lastMessage = ""
    private var lastRecordAt = 0L
    private var contextSequence = 0
    private var lastFrameGeometry = ""
    private var lastDwsRecordAt=0L
    private var lastDwsState=""
    private var lastDwsShotAt=0L
    private var dwsSequence=0L

    @Synchronized fun initialize(context: Context) {
        appContext = context.applicationContext
        enabled = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean(PREF_ENABLED, false)
        if (enabled) ensureSessionLocked()
    }
    @Synchronized fun isEnabled(context: Context? = appContext): Boolean {
        if (appContext == null && context != null) initialize(context)
        return enabled
    }
    @Synchronized fun setEnabled(context: Context, value: Boolean) {
        appContext = context.applicationContext
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean(PREF_ENABLED, value).apply()
        if (!value) record("SESSION", "diagnostic mode disabled")
        enabled = value
        sessionDir = null
        pendingScreenshot = null
        actionShots.clear()
        contextSequence = 0; lastFrameGeometry = ""
        lastActionSampleAt=0L;actionSequence=0;lastScreenshotAt=0L
        lastDwsRecordAt=0L;lastDwsState="";lastDwsShotAt=0L;dwsSequence=0L
        if (value) { ensureSessionLocked(); record("SESSION", "diagnostic mode enabled") }
    }
    @Synchronized fun record(area: String, message: String) {
        if (!enabled) return
        val now = SystemClock.elapsedRealtime()
        if (area == lastArea && message == lastMessage && now - lastRecordAt < 5_000L) return
        lastArea = area; lastMessage = message; lastRecordAt = now
        val dir = ensureSessionLocked() ?: return
        runCatching {
            val safeArea = area.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(40)
            val safeMessage = message.replace('\n', ' ').replace('\r', ' ').take(500)
            val log=File(dir,"events.log")
            rotate(log,2_000_000L)
            log.appendText("${Instant.now()} elapsed=${SystemClock.elapsedRealtime()} [$safeArea] $safeMessage\n")
        }
    }
    /** Every controller decision is tied to its actual analysis frame. Sparse lossless HUD
     * crops preserve outlines/comma separators which a 540px JPEG cannot resolve. */
    @Synchronized fun recordDwsFrame(frame:Bitmap,details:Map<String,Any?>,hudTop:Int?=null) {
        if(!enabled)return
        val now=SystemClock.elapsedRealtime()
        val state=details["state"].toString()
        val important=state=="ACTION" || state=="GRID_RECHECK_BLOCKED" || state=="RESOURCE_RESULT"
        val forceShot=state=="GRID_RECHECK_BLOCKED" && state!=lastDwsState ||
            state=="RESOURCE_RESULT" && details["result"]=="no_visual_progress" && now-lastDwsShotAt>=2_000L
        if(!important && state==lastDwsState && now-lastDwsRecordAt<1_000L)return
        val dir=ensureSessionLocked() ?: return
        lastDwsState=state;lastDwsRecordAt=now
        runCatching {
            val payload=org.json.JSONObject().apply {
                put("schema",2);put("at",Instant.now().toString());put("elapsed",now)
                details.forEach { (key,value)->put(key,org.json.JSONObject.wrap(value)) }
            }
            if(forceShot || now-lastDwsShotAt>=8_000L) {
                val stem="dws-${++dwsSequence}-${details["frameId"]}"
                val shots=File(dir,"screens").also {it.mkdirs()}
                val tw=minOf(720,frame.width);val th=maxOf(1,frame.height*tw/frame.width)
                val small=Bitmap.createScaledBitmap(frame,tw,th,true)
                try {FileOutputStream(File(shots,"$stem-before.jpg")).use {small.compress(Bitmap.CompressFormat.JPEG,78,it)}}
                finally {if(small!==frame)small.recycle()}
                payload.put("beforeImage","screens/$stem-before.jpg")
                payload.put("beforeImageSize",org.json.JSONArray(listOf(tw,th)))
                if(hudTop!=null && hudTop in 0 until frame.height) {
                    val nativeDir=File(dir,"native-hud").also {it.mkdirs()}
                    val crop=Bitmap.createBitmap(frame,0,hudTop,maxOf(1,frame.width/2),frame.height-hudTop)
                    try {FileOutputStream(File(nativeDir,"$stem.png")).use {crop.compress(Bitmap.CompressFormat.PNG,100,it)}}
                    finally {if(crop!==frame)crop.recycle()}
                    payload.put("nativeHud","native-hud/$stem.png")
                    payload.put("nativeHudRect",org.json.JSONArray(listOf(0,hudTop,frame.width/2,frame.height)))
                    trimEvidence(nativeDir,12,8_000_000L)
                }
                trimEvidence(shots,MAX_SCREENSHOTS,12_000_000L)
                lastDwsShotAt=now
            }
            val text=payload.toString()
            File(dir,"dws-current.json").writeText(text)
            val trace=File(dir,"dws-trace.jsonl")
            rotate(trace,1_000_000L)
            trace.appendText("$text\n")
        }.onFailure {record("DWS.EVIDENCE_FAILED",it.javaClass.simpleName)}
    }
    private fun rotate(file:File,budget:Long) {
        if(file.length()<budget)return
        val previous=File(file.parentFile,"${file.name}.previous")
        previous.delete()
        check(file.renameTo(previous)) {"diagnostic log rotation failed"}
    }
    private fun trimEvidence(dir:File,count:Int,bytes:Long) {
        val files=dir.listFiles()?.filter {it.isFile} ?: return
        val remove=evidenceToRemove(files.map {EvidenceFile(it.name,it.lastModified(),it.length())},count,bytes)
        files.filter {it.name in remove}.forEach {it.delete()}
    }
    @Synchronized fun requestScreenshot(reason: String) { if (enabled) pendingScreenshot = reason.take(80) }
    /** Always retain the latest mapping, even when the general context snapshot budget is used. */
    @Synchronized fun recordCalibration(extra:Map<String,String>) {
        if(!enabled)return
        val context=appContext ?: return;val directory=ensureSessionLocked() ?: return
        runCatching {DiagnosticContext.write(context,directory,"geometry-current.json",extra)}
            .onFailure {record("GEOMETRY.CONTEXT_FAILED",it.javaClass.simpleName)}
        snapshotContext("geometry",extra)
    }
    @Synchronized fun snapshotContext(reason: String, extra: Map<String, String> = emptyMap()) {
        if (!enabled || contextSequence >= 20) return
        val context = appContext ?: return
        val dir = ensureSessionLocked() ?: return
        val label = reason.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(40)
        val name = "context-${++contextSequence}-$label.json"
        runCatching { DiagnosticContext.write(context, dir, name, extra) }
            .onSuccess { record("CONTEXT", "saved=$name") }
            .onFailure { record("CONTEXT", "failed=$label type=${it.javaClass.simpleName}") }
    }
    /** Sparse frames correlated with a tap dispatch; the first frame is not guaranteed pre-tap. */
    @Synchronized fun sampleAction(kind: String, x: Int, y: Int):Int? {
        if (!enabled) return null
        val now = SystemClock.elapsedRealtime()
        if (now - lastActionSampleAt < 8_000L || actionShots.isNotEmpty()) return null
        lastActionSampleAt = now
        val id = ++actionSequence
        val label = "action-$id-$kind-$x-$y"
        val generation=de.robinthor.digiworldexplorer.capture.FrameGeometryRegistry.forCaller(now)?.generation
        actionShots.addLast(ActionShot("$label-first-frame", now,x,y,generation))
        actionShots.addLast(ActionShot("$label-followup", now + 900L,x,y,generation))
        record("ACTION_EVIDENCE", "id=$id kind=$kind target=$x,$y first=next-frame followup=900ms")
        return id
    }
    @Synchronized fun captureIfRequested(image: Image, width: Int, height: Int) {
        val now = SystemClock.elapsedRealtime()
        if (!enabled) return
        val planeGeometry = image.planes.firstOrNull()
        val geometry = "analysis=${width}x$height image=${image.width}x${image.height} rowStride=${planeGeometry?.rowStride} pixelStride=${planeGeometry?.pixelStride}"
        if (geometry != lastFrameGeometry) {
            lastFrameGeometry = geometry
            snapshotContext("capture-frame", mapOf("geometry" to geometry))
        }
        val requested = pendingScreenshot
        val action = actionShots.firstOrNull()?.takeIf { now >= it.dueAt }
        val urgent = requested != null && now - lastScreenshotAt >= SCREENSHOT_COOLDOWN
        val reason = when { urgent -> requireNotNull(requested); action != null -> action.reason; else -> return }
        val dir = ensureSessionLocked() ?: return
        val shots = File(dir, "screens").also { it.mkdirs() }
        val plane = image.planes.firstOrNull() ?: return
        if (plane.pixelStride < 3) return
        val sourceW = minOf(width, image.width); val sourceH = minOf(height, image.height)
        val targetW = minOf(540, sourceW); val targetH = (sourceH.toDouble() * targetW / sourceW).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
        val pixels = IntArray(targetW * targetH); val buffer = plane.buffer
        for (ty in 0 until targetH) { val sy = ty * sourceH / targetH
            for (tx in 0 until targetW) { val sx = tx * sourceW / targetW; val offset = sy * plane.rowStride + sx * plane.pixelStride
                if (offset + 2 < buffer.limit()) pixels[ty * targetW + tx] = Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
            }
        }
        bitmap.setPixels(pixels, 0, targetW, 0, 0, targetW, targetH)
        val safe = reason.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(36)
        runCatching {
            val name="${stamp.format(Instant.now())}-$safe.jpg"
            FileOutputStream(File(shots,name)).use {bitmap.compress(Bitmap.CompressFormat.JPEG,68,it)}
            val origin=de.robinthor.digiworldexplorer.capture.FrameGeometryRegistry.forCaller(now)
            val canonical=de.robinthor.digiworldexplorer.capture.FrameGeometryRegistry.callerAnalysisSize()==
                de.robinthor.digiworldexplorer.capture.PixelSize(sourceW,sourceH)
            val metadata=org.json.JSONObject().apply {
                put("at",Instant.now().toString());put("reason",reason);put("image","screens/$name")
                put("sourceSize",org.json.JSONArray(listOf(sourceW,sourceH)))
                put("imageSize",org.json.JSONArray(listOf(targetW,targetH)))
                put("coordinates",if(canonical)"analysis" else "raw_unconfirmed_capture")
                put("generation",origin?.generation ?: org.json.JSONObject.NULL)
                put("mapping",origin?.geometry?.toString() ?: org.json.JSONObject.NULL)
            }
            if(!urgent && action!=null && canonical && origin!=null && origin.generation==action.generation &&
                action.x in 0 until sourceW && action.y in 0 until sourceH) {
                val radius=maxOf(32,minOf(sourceW,sourceH)*16/100)
                val left=maxOf(0,action.x-radius);val top=maxOf(0,action.y-radius)
                val cw=minOf(sourceW,action.x+radius)-left;val ch=minOf(sourceH,action.y+radius)-top
                val native=Bitmap.createBitmap(cw,ch,Bitmap.Config.ARGB_8888)
                try {
                    val nativePixels=IntArray(cw*ch)
                    for(y in 0 until ch)for(x in 0 until cw){
                        val offset=(top+y)*plane.rowStride+(left+x)*plane.pixelStride
                        nativePixels[y*cw+x]=if(offset+2<buffer.limit())Color.rgb(buffer.get(offset).toInt() and 255,buffer.get(offset+1).toInt() and 255,buffer.get(offset+2).toInt() and 255) else Color.BLACK
                    }
                    native.setPixels(nativePixels,0,cw,0,0,cw,ch)
                    val nativeDir=File(dir,"native-target").also {it.mkdirs()}
                    val nativeName=name.removeSuffix(".jpg")+".png"
                    FileOutputStream(File(nativeDir,nativeName)).use {native.compress(Bitmap.CompressFormat.PNG,100,it)}
                    metadata.put("nativeTarget","native-target/$nativeName")
                    metadata.put("nativeTargetRect",org.json.JSONArray(listOf(left,top,left+cw,top+ch)))
                    metadata.put("actionTarget",org.json.JSONArray(listOf(action.x,action.y)))
                    trimEvidence(nativeDir,12,8_000_000L)
                } finally {native.recycle()}
            }
            val trace=File(dir,"action-frames.jsonl");rotate(trace,300_000L)
            trace.appendText(metadata.toString()+"\n")
            lastScreenshotAt = now
            if (urgent) pendingScreenshot = null else actionShots.removeFirstOrNull()
            record("SCREENSHOT", "saved reason=$reason size=${targetW}x$targetH") }
        trimEvidence(shots,MAX_SCREENSHOTS,12_000_000L)
        bitmap.recycle()
    }
    @Synchronized fun sessions(context: Context): List<DiagnosticSession> {
        if (appContext == null) initialize(context)
        return root(context).listFiles { f -> f.isDirectory }?.map { d -> DiagnosticSession(d, d.name, d.walkTopDown().filter { it.isFile }.sumOf { it.length() }) }?.sortedByDescending { it.name } ?: emptyList()
    }
    @Synchronized fun delete(session: DiagnosticSession): Boolean { if (session.directory == sessionDir) sessionDir = null; return session.directory.deleteRecursively() }
    @Synchronized fun shareIntent(context: Context, session: DiagnosticSession): android.content.Intent {
        writeCoverage(session.directory)
        val zip = File(context.cacheDir, "${session.name}.zip")
        ZipOutputStream(FileOutputStream(zip)).use { out -> session.directory.walkTopDown().filter { it.isFile }.forEach { file -> out.putNextEntry(ZipEntry(file.relativeTo(session.directory).invariantSeparatorsPath)); file.inputStream().use { it.copyTo(out) }; out.closeEntry() } }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", zip)
        return android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "application/zip"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    @Synchronized fun shareAllIntent(context: Context): android.content.Intent? {
        val available = sessions(context)
        if (available.isEmpty()) return null
        val zip = File(context.cacheDir, "DigiWorldExplorer-diagnostics-all.zip")
        ZipOutputStream(FileOutputStream(zip)).use { out ->
            available.forEach { session ->
                writeCoverage(session.directory)
                session.directory.walkTopDown().filter { it.isFile }.forEach { file ->
                    out.putNextEntry(ZipEntry("${session.name}/${file.relativeTo(session.directory).invariantSeparatorsPath}"))
                    file.inputStream().use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", zip)
        return android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "application/zip"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    /** Tell the recipient what exists AND what is missing; sparse evidence is not a
     * continuous screen recording and gesture completion does not prove game acceptance. */
    private fun writeCoverage(dir:File) {
        runCatching {
            val files=dir.walkTopDown().filter {it.isFile && it.name!="coverage.json"}.toList()
            val json=org.json.JSONObject().apply {
                put("schema",2);put("exportedAt",Instant.now().toString())
                put("session",dir.name)
                put("files",org.json.JSONArray(files.map {org.json.JSONObject().apply {
                    put("path",it.relativeTo(dir).invariantSeparatorsPath);put("bytes",it.length())
                }}))
                put("hasDwsDecisionTrace",File(dir,"dws-trace.jsonl").exists())
                put("retainedScreenshots",files.count {it.parentFile?.name=="screens"})
                put("retainedNativeHudCrops",files.count {it.parentFile?.name=="native-hud"})
                put("retainedNativeTargetCrops",files.count {it.parentFile?.name=="native-target"})
                put("limits",org.json.JSONObject().apply {
                    put("screenFiles",MAX_SCREENSHOTS);put("screenBytes",12_000_000)
                    put("nativeHudFiles",12);put("nativeHudBytes",8_000_000)
                    put("nativeTargetFiles",12);put("nativeTargetBytes",8_000_000)
                    put("dwsTraceBytesPerPart",1_000_000);put("eventBytesPerPart",2_000_000)
                    put("logParts",2);put("olderEvidenceMayBeEvicted",true)
                })
                put("interpretation", "DWS images are pre-decision analysis frames. Gesture samples are next-frame/900ms, not guaranteed pre-tap. Input completed is not visual success. DWS board signature change is a heuristic, not verified distance. Images/traces are sampled and bounded; inspect manifest for retained files. No phone compatibility guarantee.")
            }
            File(dir,"coverage.json").writeText(json.toString(2))
        }
    }
    private fun root(context: Context) = (context.getExternalFilesDir(ROOT) ?: File(context.filesDir, ROOT)).also { it.mkdirs() }
    private fun ensureSessionLocked(): File? {
        val context = appContext ?: return null; sessionDir?.let { return it }; val root = root(context)
        val dir = File(root, "diagnostic-${stamp.format(Instant.now())}").also { it.mkdirs() }; sessionDir = dir
        File(dir, "events.log").appendText("${Instant.now()} [SESSION] app=${BuildConfig.VERSION_NAME} device=${Build.MANUFACTURER}/${Build.MODEL} android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}\n")
        runCatching { DiagnosticContext.write(context, dir, "context-session.json", emptyMap()) }
        File(dir,"README.txt").writeText("Diagnostic schema 2. Opt-in game screenshots may contain personal information: review before sharing. context-session/context-*/geometry-current describe device, whitelisted settings and capture mapping (no activation code). events.log + .previous contain recent input/status history. dws-current.json and dws-trace.jsonl + .previous contain DWS grid/cell scores, stock/unknown glyphs, wait/plan reason and analysis/display targets with frame/generation IDs. Native HUD PNGs retain original pixels: nativeHudRect gives their position in the analysis frame. Screen JPEGs are scaled with dimensions in trace. Only sampled frames are saved; older evidence is removed to retain the end of long runs. coverage.json is generated at export, listing what is actually included. Other features retain their existing status/gesture snapshots, not the new DWS-specific decision trace. Input callbacks are not game-success proof. Disable USB debugging for physical-phone gameplay.\n")
        File(dir,"README.txt").appendText("action-frames.jsonl (+ .previous) maps general gesture screenshots to analysis/raw coordinates, source and saved image sizes, and geometry generation. native-target PNGs show sampled first/followup tap surroundings at original resolution; nativeTargetRect locates the crop. They are deliberately omitted when geometry changed or is unconfirmed. These followups are not proof of success. Native files are separately bounded to 12 files/8 MB per category.\n")
        root.listFiles { f -> f.isDirectory }?.sortedByDescending { it.name }?.drop(MAX_SESSIONS)?.forEach { it.deleteRecursively() }
        return dir
    }
}
