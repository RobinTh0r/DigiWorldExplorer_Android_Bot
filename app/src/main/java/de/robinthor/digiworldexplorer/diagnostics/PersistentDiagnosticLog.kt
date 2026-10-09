package de.robinthor.digiworldexplorer.diagnostics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.Image
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
    private data class ActionShot(val reason: String, val dueAt: Long)
    private val actionShots = ArrayDeque<ActionShot>()
    private var lastActionSampleAt = 0L
    private var actionSequence = 0
    private var lastScreenshotAt = 0L
    private var lastArea = ""
    private var lastMessage = ""
    private var lastRecordAt = 0L
    private var contextSequence = 0
    private var lastFrameGeometry = ""

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
            File(dir, "events.log").appendText("${Instant.now()} elapsed=${SystemClock.elapsedRealtime()} [$safeArea] $safeMessage\n")
        }
    }
    @Synchronized fun requestScreenshot(reason: String) { if (enabled) pendingScreenshot = reason.take(80) }
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
    @Synchronized fun sampleAction(kind: String, x: Int, y: Int) {
        if (!enabled) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastActionSampleAt < 8_000L || actionShots.isNotEmpty()) return
        lastActionSampleAt = now
        val id = ++actionSequence
        val label = "action-$id-$kind-$x-$y"
        actionShots.addLast(ActionShot("$label-first-frame", now))
        actionShots.addLast(ActionShot("$label-followup", now + 900L))
        record("ACTION_EVIDENCE", "id=$id kind=$kind target=$x,$y first=next-frame followup=900ms")
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
        val reason = when { urgent -> requested!!; action != null -> action.reason; else -> return }
        val dir = ensureSessionLocked() ?: return
        val shots = File(dir, "screens").also { it.mkdirs() }
        if ((shots.listFiles()?.size ?: 0) >= MAX_SCREENSHOTS) { pendingScreenshot = null; actionShots.clear(); return }
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
        runCatching { FileOutputStream(File(shots, "${stamp.format(Instant.now())}-$safe.jpg")).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 68, it) }
            lastScreenshotAt = now
            if (urgent) pendingScreenshot = null else actionShots.removeFirstOrNull()
            record("SCREENSHOT", "saved reason=$reason size=${targetW}x$targetH") }
        bitmap.recycle()
    }
    @Synchronized fun sessions(context: Context): List<DiagnosticSession> {
        if (appContext == null) initialize(context)
        return root(context).listFiles { f -> f.isDirectory }?.map { d -> DiagnosticSession(d, d.name, d.walkTopDown().filter { it.isFile }.sumOf { it.length() }) }?.sortedByDescending { it.name } ?: emptyList()
    }
    @Synchronized fun delete(session: DiagnosticSession): Boolean { if (session.directory == sessionDir) sessionDir = null; return session.directory.deleteRecursively() }
    @Synchronized fun shareIntent(context: Context, session: DiagnosticSession): android.content.Intent {
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
    private fun root(context: Context) = (context.getExternalFilesDir(ROOT) ?: File(context.filesDir, ROOT)).also { it.mkdirs() }
    private fun ensureSessionLocked(): File? {
        val context = appContext ?: return null; sessionDir?.let { return it }; val root = root(context)
        val dir = File(root, "diagnostic-${stamp.format(Instant.now())}").also { it.mkdirs() }; sessionDir = dir
        File(dir, "events.log").appendText("${Instant.now()} [SESSION] app=${BuildConfig.VERSION_NAME} device=${Build.MANUFACTURER}/${Build.MODEL} android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}\n")
        runCatching { DiagnosticContext.write(context, dir, "context-session.json", emptyMap()) }
        root.listFiles { f -> f.isDirectory }?.sortedByDescending { it.name }?.drop(MAX_SESSIONS)?.forEach { it.deleteRecursively() }
        return dir
    }
}
