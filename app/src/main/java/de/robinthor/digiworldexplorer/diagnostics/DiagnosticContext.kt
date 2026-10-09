package de.robinthor.digiworldexplorer.diagnostics

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import de.robinthor.digiworldexplorer.BuildConfig
import de.robinthor.digiworldexplorer.strategy.AutomationState
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId

/** Read explicit diagnostic settings only; activation strings/account identifiers are excluded. */
internal val diagnosticSettingDefaults: Map<String, Any> = linkedMapOf(
    "ad_skip_pass" to false, "dungeon_use_ads" to true, "dungeon_normal_attempts" to 3,
    "automation_mode" to "SEMI_AUTO", "dws_profile" to "V4_DASH",
    "legacy_capture" to true, "grid_enabled" to true, "quick_overlay_enabled" to false,
    "director_card_visible" to true, "summon_touch_correction" to false,
    "auto_purchase" to false, "auto_dungeon" to false, "auto_network_defense" to false,
    "auto_feed" to false, "auto_bond_rotation" to true, "auto_farm_harvest" to true,
    "copilot_rewards" to true, "copilot_dws" to true, "farm_watering" to true,
    "dungeon_card_apocalymon_wall" to true, "dungeon_card_demidevimon" to true,
    "dungeon_card_bakemon" to true, "dungeon_card_digifactory" to true,
    "dungeon_card_network_defense" to true, "dungeon_card_metal_sea" to true,
    "dungeon_card_daily" to true,
)

internal fun collectDiagnosticSettings(read: (String, Any) -> Any): Map<String, Any> =
    diagnosticSettingDefaults.mapValues { (key, default) -> read(key, default) }

internal object DiagnosticContext {
    @Suppress("DEPRECATION")
    fun write(context: Context, directory: File, name: String, extra: Map<String, String>) {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val settings = collectDiagnosticSettings { key, default ->
            when (default) {
                is Boolean -> preferences.getBoolean(key, default)
                is Int -> preferences.getInt(key, default)
                else -> preferences.getString(key, default.toString()) ?: default
            }
        }
        val metrics = context.resources.displayMetrics
        val manager = context.getSystemService(WindowManager::class.java)
        val real = DisplayMetrics().also { manager.defaultDisplay.getRealMetrics(it) }
        val device = JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER); put("model", Build.MODEL)
            put("brand", Build.BRAND); put("device", Build.DEVICE); put("product", Build.PRODUCT)
            put("android", Build.VERSION.RELEASE); put("sdk", Build.VERSION.SDK_INT)
            put("displayWidth", real.widthPixels); put("displayHeight", real.heightPixels)
            put("appWidth", metrics.widthPixels); put("appHeight", metrics.heightPixels)
            put("densityDpi", metrics.densityDpi); put("density", metrics.density)
            put("fontScale", context.resources.configuration.fontScale)
            put("orientation", context.resources.configuration.orientation)
            put("rotation", manager.defaultDisplay.rotation)
            put("locale", context.resources.configuration.locales[0].toLanguageTag())
            put("insetsSupported", Build.VERSION.SDK_INT >= 30)
            if (Build.VERSION.SDK_INT >= 30) {
                val window = manager.currentWindowMetrics
                val insets = window.windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                put("windowWidth", window.bounds.width()); put("windowHeight", window.bounds.height())
                put("insets", JSONObject().apply {
                    put("left", insets.left); put("top", insets.top)
                    put("right", insets.right); put("bottom", insets.bottom)
                })
            }
        }
        val runtime = JSONObject().apply {
            put("enabled", AutomationState.enabled); put("mode", AutomationState.mode.name)
            put("adSkipPass", AutomationState.adSkipPassEnabled)
            put("legacyCapture", AutomationState.forceLegacyCaptureMetrics)
            put("dwsRules", AutomationState.dwsNavigationSettings.toString())
            put("summon", AutomationState.autoPurchaseEnabled); put("vsLoop", AutomationState.autoDungeonEnabled)
            put("network", AutomationState.autoNetworkDefenseEnabled); put("feed", AutomationState.autoFeedEnabled)
            put("bond", AutomationState.autoBondRotationEnabled); put("farm", AutomationState.autoFarmEnabled)
            put("copilotRewards", AutomationState.copilotRewardsEnabled); put("copilotDws", AutomationState.copilotDwsEnabled)
        }
        val json = JSONObject().apply {
            put("schema", 1); put("at", Instant.now().toString()); put("timezone", ZoneId.systemDefault().id)
            put("appVersion", BuildConfig.VERSION_NAME); put("versionCode", BuildConfig.VERSION_CODE)
            put("device", device); put("settings", JSONObject(settings)); put("runtime", runtime)
            put("savedKeys", org.json.JSONArray(settings.keys.filter { preferences.contains(it) }))
            put("observation", JSONObject(extra))
        }
        File(directory, name).writeText(json.toString(2))
    }
}
