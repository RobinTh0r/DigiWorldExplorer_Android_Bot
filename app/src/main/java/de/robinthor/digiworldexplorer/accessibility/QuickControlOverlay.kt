package de.robinthor.digiworldexplorer.accessibility

import android.content.Context
import android.content.Intent
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import de.robinthor.digiworldexplorer.MainActivity
import de.robinthor.digiworldexplorer.CaptureConsentActivity
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.capture.CaptureFrameAnalyzer
import de.robinthor.digiworldexplorer.capture.CaptureSessionState
import de.robinthor.digiworldexplorer.capture.ScreenCaptureService
import de.robinthor.digiworldexplorer.dungeon.DungeonFrameAnalyzer
import de.robinthor.digiworldexplorer.dungeon.DungeonRotationRequest
import de.robinthor.digiworldexplorer.feed.FeedFrameAnalyzer
import de.robinthor.digiworldexplorer.feed.StageFailedFrameAnalyzer
import de.robinthor.digiworldexplorer.license.SupporterLicenseManager
import de.robinthor.digiworldexplorer.network.NetworkDefenseFrameAnalyzer
import de.robinthor.digiworldexplorer.purchase.RewardPurchaseFrameAnalyzer
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.automation.DirectorSnapshot
import kotlin.math.abs

/** Small user-controlled overlay. The full-screen grid remains non-touchable. */
class QuickControlOverlay(private val service: DigiWorldAccessibilityService) {
    /** Only opaque/touchable controls, never the transparent full-screen grid window. */
    fun occupiedBounds():List<de.robinthor.digiworldexplorer.capture.PixelRect> = listOfNotNull(root,copilotBadge).mapNotNull { view ->
        if(view.visibility!=View.VISIBLE || !view.isAttachedToWindow || view.width<=0 || view.height<=0)return@mapNotNull null
        val location=IntArray(2);view.getLocationOnScreen(location)
        de.robinthor.digiworldexplorer.capture.PixelRect(location[0],location[1],location[0]+view.width,location[1]+view.height)
    }
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val preferences = service.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private var root: LinearLayout? = null
    private var panel: LinearLayout? = null
    private var bubble: View? = null
    private var copilotBadge: View? = null
    private var copilotBadgeParams: WindowManager.LayoutParams? = null
    private var copilotArrowAnimator: ObjectAnimator? = null
    private var directorTitle: TextView? = null
    private var directorAction: TextView? = null
    private var directorTimer: TextView? = null
    private val timerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val timerTick = object : Runnable {
        override fun run() {
            if (root == null) return
            updateTimer()
            updateAutomationControls()
            timerHandler.postDelayed(this, 1_000L)
        }
    }

    private fun updateTimer() {
        val remaining = de.robinthor.digiworldexplorer.automation.BondCycleTimer.remainingMillis()
        val seconds = (remaining + 999) / 1000
        directorTimer?.text = when {
            de.robinthor.digiworldexplorer.feed.BondRotationRequest.active() -> service.getString(R.string.quick_bond_running)
            seconds > 0 -> "Bond %02d:%02d".format(seconds / 60, seconds % 60)
            else -> service.getString(R.string.quick_bond_ready)
        }
    }
    private var directorCard: View? = null
    private var directorTail: View? = null
    private var eyeStatus: BotEyeStatusView? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private var syncingFeatureSwitches = false
    private val featureSwitches = mutableMapOf<String, Switch>()
    private var dungeonControl: Button? = null
    private var dungeonSpinner: ProgressBar? = null
    private var copilotControl: Button? = null
    private var copilotSpinner: ProgressBar? = null

    fun show() {
        if (root != null) return
        de.robinthor.digiworldexplorer.automation.BondCycleTimer.initialize(service)
        val density = service.resources.displayMetrics.density
        val container = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }
        val icon = ImageView(service).apply {
            setImageResource(R.mipmap.ic_launcher_round)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = service.getString(R.string.quick_overlay_open_controls)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(27, 30, 36))
                setStroke((1.5f * density).toInt().coerceAtLeast(1), Color.rgb(50, 184, 180))
            }
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            setPadding((3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt())
            elevation = 8f * density
            layoutParams = FrameLayout.LayoutParams((52 * density).toInt(), (52 * density).toInt())
        }
        val eyes = BotEyeStatusView(service).apply {
            isClickable = false
            // The source ImageView has elevation; keep animated eyes above that bitmap.
            elevation = 12f * density
            layoutParams = FrameLayout.LayoutParams((52 * density).toInt(), (52 * density).toInt())
        }
        val iconStack = FrameLayout(service).apply {
            addView(icon); addView(eyes)
            layoutParams = LinearLayout.LayoutParams((52 * density).toInt(), (52 * density).toInt())
        }
        val title = TextView(service).apply {
            textSize = 11f; setTextColor(Color.WHITE); maxLines = 1
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val action = TextView(service).apply {
            textSize = 10f; setTextColor(Color.rgb(150, 229, 224)); maxLines = 1
        }
        val timer = TextView(service).apply {
            textSize = 9f; setTextColor(Color.rgb(210, 177, 255)); maxLines = 1
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        val actionRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(action, LinearLayout.LayoutParams(0, WindowManager.LayoutParams.MATCH_PARENT, 1f))
            addView(timer, LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.MATCH_PARENT))
        }
        val statusCard = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((10 * density).toInt(), 0, (10 * density).toInt(), 0)
            background = rounded(Color.argb(232, 16, 24, 38), 11 * density)
            addView(title, LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(actionRow, LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, 0, 1f))
            layoutParams = LinearLayout.LayoutParams((198 * density).toInt(), (52 * density).toInt())
        }
        val tail = View(service).apply {
            background = GradientDrawable().apply { setColor(Color.argb(232, 16, 24, 38)); cornerRadius = 2f * density }
            rotation = 45f
            layoutParams = LinearLayout.LayoutParams((11 * density).toInt(), (11 * density).toInt()).apply {
                marginStart = (-5 * density).toInt(); marginEnd = (-6 * density).toInt()
            }
        }
        val header = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(iconStack)
            addView(tail)
            addView(statusCard)
        }
        val menu = buildPanel(density).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams((250 * density).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        container.addView(header)
        container.addView(menu)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Settings.canDrawOverlays(service)) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val screenHeight = if (Build.VERSION.SDK_INT >= 30) windowManager.maximumWindowMetrics.bounds.height()
                else service.resources.displayMetrics.heightPixels
            x = preferences.getInt("quick_overlay_x", (8 * density).toInt())
            y = preferences.getInt("quick_overlay_y", (screenHeight * .15f).toInt())
        }
        val cardVisible = preferences.getBoolean("director_card_visible", true)
        statusCard.visibility = if (cardVisible && !DungeonRotationRequest.ownsFrames()) View.VISIBLE else View.GONE
        tail.visibility = statusCard.visibility
        installDragAndClick(iconStack, layoutParams, onLongPress = {
            val visible = !preferences.getBoolean("director_card_visible", true)
            preferences.edit().putBoolean("director_card_visible", visible).apply()
            updateDirectorCardDisplay()
            root?.requestLayout()
        }) { togglePanel(layoutParams) }
        installDragAndClick(statusCard, layoutParams) { }
        root = container
        panel = menu
        bubble = iconStack
        directorTitle = title
        directorAction = action
        directorTimer = timer
        directorCard = statusCard
        directorTail = tail
        eyeStatus = eyes
        params = layoutParams
        runCatching { windowManager.addView(container, layoutParams) }
        installCopilotBadge(density, layoutParams)
        refresh()
        updateDirector(de.robinthor.digiworldexplorer.automation.ScreenDirector.snapshot())
        timerHandler.post(timerTick)
    }

    fun destroy() {
        timerHandler.removeCallbacks(timerTick)
        copilotArrowAnimator?.cancel()
        copilotArrowAnimator = null
        copilotBadge?.let { runCatching { windowManager.removeView(it) } }
        copilotBadge = null
        copilotBadgeParams = null
        directorTimer = null
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        panel = null
        bubble = null
        directorTitle = null
        directorAction = null
        directorCard = null
        directorTail = null
        eyeStatus = null
        dungeonControl = null
        dungeonSpinner = null
        copilotControl = null
        copilotSpinner = null
        params = null
    }

    fun setDirectorCardVisible(visible: Boolean) {
        preferences.edit().putBoolean("director_card_visible", visible).apply()
        root?.post {
            updateDirectorCardDisplay()
            root?.requestLayout()
            params?.let { layout -> root?.let { runCatching { windowManager.updateViewLayout(it, layout) } } }
        }
    }

    private fun updateDirectorCardDisplay() {
        val visible = preferences.getBoolean("director_card_visible", true) &&
            !DungeonRotationRequest.ownsFrames()
        directorCard?.visibility = if (visible) View.VISIBLE else View.GONE
        directorTail?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun installCopilotBadge(density: Float, anchor: WindowManager.LayoutParams) {
        val arrow = TextView(service).apply {
            text = "↻"
            textSize = 15f
            setTextColor(Color.rgb(226, 184, 255))
            gravity = Gravity.CENTER
        }
        val label = TextView(service).apply {
            text = service.getString(R.string.quick_copilot_badge)
            textSize = 9f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val badge = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding((3 * density).toInt(), 0, (3 * density).toInt(), 0)
            background = GradientDrawable().apply {
                setColor(Color.argb(195, 19, 23, 37))
                cornerRadius = 9 * density
                setStroke((1 * density).toInt().coerceAtLeast(1), Color.rgb(176, 103, 245))
            }
            addView(arrow, LinearLayout.LayoutParams((16 * density).toInt(),
                WindowManager.LayoutParams.MATCH_PARENT))
            addView(label)
            visibility = View.GONE
        }
        val badgeParams = WindowManager.LayoutParams(
            (82 * density).toInt(), (18 * density).toInt(),
            if (Settings.canDrawOverlays(service)) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Android 12+ rejects pass-through touches beneath opaque untrusted overlays.
            alpha = .75f
            x = anchor.x
            y = anchor.y + (54 * density).toInt()
        }
        if (runCatching { windowManager.addView(badge, badgeParams) }.isSuccess) {
            copilotBadge = badge
            copilotBadgeParams = badgeParams
            moveCopilotBadge(anchor)
            copilotArrowAnimator = ObjectAnimator.ofFloat(arrow, View.ROTATION, 0f, 360f).apply {
                duration = 1_400L
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
        }
    }

    private fun moveCopilotBadge(anchor: WindowManager.LayoutParams) {
        val badge = copilotBadge ?: return
        val params = copilotBadgeParams ?: return
        val bounds = if (Build.VERSION.SDK_INT >= 30) windowManager.maximumWindowMetrics.bounds
            else android.graphics.Rect(0, 0, service.resources.displayMetrics.widthPixels,
                service.resources.displayMetrics.heightPixels)
        val density = service.resources.displayMetrics.density
        params.x = anchor.x.coerceIn(0, (bounds.width() - params.width).coerceAtLeast(0))
        val below = anchor.y + (54 * density).toInt()
        params.y = (if (below + params.height <= bounds.height()) below
            else anchor.y - params.height - (2 * density).toInt())
            .coerceIn(0, (bounds.height() - params.height).coerceAtLeast(0))
        runCatching { windowManager.updateViewLayout(badge, params) }
    }

    fun refresh() {
        root?.post {
            bubble?.alpha = if (AutomationState.enabled) 1f else .78f
        }
    }

    fun updateDirector(snapshot: DirectorSnapshot) {
        root?.post {
            val captureActive = CaptureSessionState.snapshot(AutomationState.enabled).captureActive
            directorTitle?.text = if (!captureActive) service.getString(R.string.quick_capture_stopped) else "${snapshot.copilot.ifBlank { snapshot.state }} · ${snapshot.screen.label}"
            val idle = snapshot.action.isBlank() || snapshot.action == "No action"
            val value = when {
                !captureActive -> service.getString(R.string.quick_start_restart)
                idle && de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.active() -> service.getString(R.string.quick_wait_next_rotation)
                idle -> service.getString(R.string.quick_no_action)
                else -> snapshot.action
            }
            directorAction?.text = if (value.length > 20) value.take(19) + "…" else value
            updateTimer()
            eyeStatus?.update(snapshot)
        }
    }

    private fun buildPanel(density: Float): LinearLayout = LinearLayout(service).apply {
        orientation = LinearLayout.VERTICAL
        setPadding((8 * density).toInt(), (6 * density).toInt(), (8 * density).toInt(), (6 * density).toInt())
        background = rounded(Color.argb(248, 27, 30, 36), 16 * density)
        elevation = 10f * density
        addView(featureToggle(service.getString(R.string.quick_auto_summon), "auto_purchase", AutomationState.autoPurchaseEnabled, density))
        addView(featureToggle(service.getString(R.string.auto_feed), "auto_feed", AutomationState.autoFeedEnabled, density))
        addView(featureToggle(service.getString(R.string.auto_network_defense), "auto_network_defense", AutomationState.autoNetworkDefenseEnabled, density))
        addView(featureToggle(service.getString(R.string.auto_dungeon), "auto_dungeon", AutomationState.autoDungeonEnabled, density))
        addView(actionButton(service.getString(R.string.quick_status_toggle)) { toggleDirectorCard(); collapse() },
            LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, (36 * density).toInt()).apply { bottomMargin = (4 * density).toInt() })
        val topRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        topRow.addView(actionButton(service.getString(R.string.quick_bot_app)) { openMainApp(); collapse() }, LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f).apply { marginEnd = (3 * density).toInt() })
        topRow.addView(actionButton(service.getString(R.string.quick_stop), danger = true) {
            AutomationState.stop()
            ScreenCaptureService.stop(service)
            service.showStatusOnly("", false)
            service.clearCalibrationOverlay()
            service.setOverlayEnabled(false)
            service.setQuickControlsEnabled(false)
        }, LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f).apply { marginStart = (3 * density).toInt() })
        addView(topRow)
        addView(statefulAutomationButton("dungeon", density) {
            if (SupporterLicenseManager.load(service) == null) {
                service.showStatusOnly(service.getString(R.string.quick_beta_code_required))
            } else if (!AutomationState.enabled) {
                requestCaptureAndStart(CaptureConsentActivity.START_DUNGEON)
            } else if (DungeonRotationRequest.ownsFrames()) {
                DungeonRotationRequest.cancel()
                service.showStatusOnly(service.getString(R.string.quick_dungeon_copilot_stopped))
            } else {
                DungeonRotationRequest.start(service)
                service.showStatusOnly("Dungeon: ${DungeonRotationRequest.apocalymonStatus}")
            }
            updateAutomationControls()
            collapse()
        }, LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, (40 * density).toInt()).apply { topMargin = (4 * density).toInt() })
        addView(buildDigiCopilotSection(density), LinearLayout.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = (4 * density).toInt() })
        val startRow = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
        startRow.addView(actionButton(service.getString(R.string.quick_start_restart)) {
            if (CaptureSessionState.snapshot(AutomationState.enabled).captureActive) reloadAutomation() else requestCaptureAndStart()
            collapse()
        }, LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f).apply { marginEnd = (3 * density).toInt() })
        startRow.addView(actionButton(service.getString(R.string.quick_bond_reset), beta = true) {
            de.robinthor.digiworldexplorer.automation.BondCycleTimer.resetCooldown()
            if (de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.active() &&
                AutomationState.autoBondRotationEnabled) {
                de.robinthor.digiworldexplorer.feed.BondRotationRequest.start()
            }
            service.showStatusOnly(service.getString(R.string.quick_bond_timer_reset))
            updateTimer()
            collapse()
        }, LinearLayout.LayoutParams((78 * density).toInt(), (40 * density).toInt()).apply { marginStart = (3 * density).toInt() })
        addView(startRow, LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply { topMargin = (4 * density).toInt() })
    }

    private fun buildDigiCopilotSection(density: Float): LinearLayout = LinearLayout(service).apply {
        orientation = LinearLayout.VERTICAL
        setPadding((5 * density).toInt(), (5 * density).toInt(), (5 * density).toInt(), (3 * density).toInt())
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.argb(72, 176, 112, 0))
            cornerRadius = 12 * density
            setStroke((2 * density).toInt().coerceAtLeast(2), Color.rgb(255, 190, 45))
        }
        addView(statefulAutomationButton("copilot", density) {
            if (SupporterLicenseManager.load(service) == null) {
                service.showStatusOnly(service.getString(R.string.quick_beta_code_required))
            } else if (!AutomationState.enabled) {
                requestCaptureAndStart(CaptureConsentActivity.START_COPILOT)
            } else {
                if (de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.active()) {
                    de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.stop("Stopped by user")
                    de.robinthor.digiworldexplorer.feed.BondRotationRequest.cancel()
                    service.showStatusOnly(service.getString(R.string.quick_digi_copilot_stopped))
                } else if (DungeonRotationRequest.ownsFrames()) {
                    service.showStatusOnly(service.getString(R.string.quick_stop_dungeon_first))
                } else {
                    de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.start()
                    if (AutomationState.autoBondRotationEnabled &&
                        de.robinthor.digiworldexplorer.automation.BondCycleTimer.remainingMillis() == 0L) {
                        de.robinthor.digiworldexplorer.feed.BondRotationRequest.start()
                    } else if (AutomationState.autoFarmEnabled) {
                        de.robinthor.digiworldexplorer.automation.BondCycleTimer.requestFarmRecovery()
                        de.robinthor.digiworldexplorer.automation.BondFarmAnalyzer.requestVisit()
                    } else if (AutomationState.copilotRewardsEnabled) {
                        de.robinthor.digiworldexplorer.automation.HomeIdleRewardRequest.start()
                    } else if (AutomationState.copilotDwsEnabled) {
                        de.robinthor.digiworldexplorer.automation.DwsExcursionRequest.start()
                    } else {
                        de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.stop("No modules selected")
                        service.showStatusOnly(service.getString(R.string.quick_copilot_select_module))
                        collapse()
                        return@statefulAutomationButton
                    }
                    service.showStatusOnly(service.getString(R.string.quick_copilot_wait_home))
                }
            }
            updateAutomationControls()
            collapse()
        }, LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, (40 * density).toInt()))
        addView(TextView(service).apply {
            text = service.getString(R.string.quick_copilot_modules)
            textSize = 10f
            setTextColor(Color.rgb(255, 213, 110))
            setPadding((4 * density).toInt(), (4 * density).toInt(), 0, 0)
        })
        addView(featureToggle(service.getString(R.string.bond_rotation_title), "auto_bond_rotation", AutomationState.autoBondRotationEnabled, density, beta = true))
        addView(featureToggle(service.getString(R.string.meat_field_title), "auto_farm_harvest", AutomationState.autoFarmEnabled, density, beta = true))
        addView(featureToggle(service.getString(R.string.quick_home_rewards), "copilot_rewards", AutomationState.copilotRewardsEnabled, density, beta = true))
        addView(featureToggle(service.getString(R.string.quick_dws_five_minutes), "copilot_dws", AutomationState.copilotDwsEnabled, density, beta = true))
    }

    @Suppress("DEPRECATION")
    private fun featureToggle(label: String, preferenceKey: String, initialValue: Boolean, density: Float, beta: Boolean = false): LinearLayout {
        val toggle = Switch(service).apply {
            isChecked = preferences.getBoolean(preferenceKey, initialValue)
            showText = false
            thumbTintList = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Color.rgb(244, 245, 246), Color.rgb(174, 181, 190)),
            )
            trackTintList = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Color.rgb(50, 184, 180), Color.rgb(98, 104, 112)),
            )
            setOnCheckedChangeListener { _, checked ->
                if (!syncingFeatureSwitches) updateFeature(preferenceKey, checked)
            }
        }
        featureSwitches[preferenceKey] = toggle
        return LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((4 * density).toInt(), 0, 0, 0)
            addView(TextView(service).apply {
                text = if (beta) "$label  · BETA" else label
                textSize = 11f
                setTextColor(if (beta) Color.rgb(255, 202, 74) else Color.rgb(244, 245, 246))
                maxLines = 1
            }, LinearLayout.LayoutParams(0, (34 * density).toInt(), 1f).apply { gravity = Gravity.CENTER_VERTICAL })
            addView(toggle, LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, (34 * density).toInt()))
        }
    }

    private fun updateFeature(preferenceKey: String, enabled: Boolean) {
        preferences.edit().putBoolean(preferenceKey, enabled).apply()
        when (preferenceKey) {
            "auto_purchase" -> {
                AutomationState.autoPurchaseEnabled = enabled
                RewardPurchaseFrameAnalyzer.reset()
            }
            "auto_dungeon" -> {
                val allowed = enabled
                AutomationState.autoDungeonEnabled = allowed
                if (allowed != enabled) preferences.edit().putBoolean(preferenceKey, allowed).apply()
                DungeonFrameAnalyzer.reset()
                featureSwitches[preferenceKey]?.isChecked = allowed
            }
            "auto_feed" -> {
                AutomationState.autoFeedEnabled = enabled
                FeedFrameAnalyzer.reset()
            }
            "auto_farm_harvest" -> {
                val allowed = enabled && SupporterLicenseManager.load(service) != null
                AutomationState.autoFarmEnabled = allowed
                if (allowed != enabled) preferences.edit().putBoolean(preferenceKey, allowed).apply()
                de.robinthor.digiworldexplorer.farm.FarmHarvestAnalyzer.reset()
                featureSwitches[preferenceKey]?.isChecked = allowed
            }
            "copilot_dws" -> {
                val allowed = enabled && SupporterLicenseManager.load(service) != null
                AutomationState.copilotDwsEnabled = allowed
                if (allowed != enabled) preferences.edit().putBoolean(preferenceKey, allowed).apply()
                featureSwitches[preferenceKey]?.isChecked = allowed
            }
            "copilot_rewards" -> {
                val allowed = enabled && SupporterLicenseManager.load(service) != null
                AutomationState.copilotRewardsEnabled = allowed
                if (allowed != enabled) preferences.edit().putBoolean(preferenceKey, allowed).apply()
                featureSwitches[preferenceKey]?.isChecked = allowed
            }
            "auto_bond_rotation" -> {
                val allowed = enabled && SupporterLicenseManager.load(service) != null
                AutomationState.autoBondRotationEnabled = allowed
                if (allowed != enabled) preferences.edit().putBoolean(preferenceKey, allowed).apply()
                de.robinthor.digiworldexplorer.feed.BondRotationAnalyzer.reset()
                featureSwitches[preferenceKey]?.isChecked = allowed
            }
            "auto_network_defense" -> {
                AutomationState.autoNetworkDefenseEnabled = enabled
                NetworkDefenseFrameAnalyzer.reset()
                StageFailedFrameAnalyzer.reset()
            }
        }
    }

    private fun refreshFeatureSwitches() {
        syncingFeatureSwitches = true
        featureSwitches["auto_purchase"]?.isChecked = preferences.getBoolean("auto_purchase", false)
        val supporter = SupporterLicenseManager.load(service) != null
        featureSwitches["auto_dungeon"]?.isChecked = preferences.getBoolean("auto_dungeon", false)
        featureSwitches["auto_feed"]?.isChecked = preferences.getBoolean("auto_feed", false)
        featureSwitches["auto_bond_rotation"]?.isChecked = supporter && preferences.getBoolean("auto_bond_rotation", true)
        featureSwitches["auto_farm_harvest"]?.isChecked = supporter && preferences.getBoolean("auto_farm_harvest", true)
        featureSwitches["copilot_rewards"]?.isChecked = supporter && preferences.getBoolean("copilot_rewards", true)
        featureSwitches["copilot_dws"]?.isChecked = supporter && preferences.getBoolean("copilot_dws", true)
        featureSwitches["auto_network_defense"]?.isChecked = preferences.getBoolean("auto_network_defense", false)
        syncingFeatureSwitches = false
    }

    private fun actionButton(label: String, danger: Boolean = false, beta: Boolean = false, action: () -> Unit): Button = Button(service).apply {
        text = if (beta) "$label  · BETA" else label
        textSize = 10.5f
        isAllCaps = false
        setTextColor(Color.WHITE)
        backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (danger) Color.rgb(230, 92, 97) else if (beta) Color.rgb(176, 112, 0) else Color.rgb(20, 127, 130)
        )
        setOnClickListener { action() }
    }

    private fun statefulAutomationButton(kind: String, density: Float, action: () -> Unit): FrameLayout {
        val button = actionButton("", beta = true, action = action).apply {
            setPadding((8 * density).toInt(), 0, (32 * density).toInt(), 0)
        }
        val spinner = ProgressBar(service).apply {
            isIndeterminate = true
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            visibility = View.GONE
        }
        if (kind == "dungeon") { dungeonControl = button; dungeonSpinner = spinner }
        else { copilotControl = button; copilotSpinner = spinner }
        return FrameLayout(service).apply {
            addView(button, FrameLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT))
            addView(spinner, FrameLayout.LayoutParams((22 * density).toInt(), (22 * density).toInt(), Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = (8 * density).toInt()
            })
            post { updateAutomationControls() }
        }
    }

    private fun updateAutomationControls() {
        val dungeonRunning = DungeonRotationRequest.ownsFrames()
        updateDirectorCardDisplay()
        dungeonControl?.apply {
            text = service.getString(if (dungeonRunning) R.string.quick_dungeon_stop else R.string.quick_dungeon_start)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (dungeonRunning) Color.rgb(190, 55, 76) else Color.rgb(176, 112, 0))
        }
        dungeonSpinner?.visibility = if (dungeonRunning) View.VISIBLE else View.GONE
        val copilotRunning = de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.active()
        copilotControl?.apply {
            text = service.getString(if (copilotRunning) R.string.quick_copilot_stop else R.string.quick_copilot_start)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (copilotRunning) Color.rgb(104, 67, 180) else Color.rgb(176, 112, 0))
        }
        copilotSpinner?.visibility = if (copilotRunning) View.VISIBLE else View.GONE
        val showCopilotBadge = copilotRunning && !dungeonRunning
        copilotBadge?.visibility = if (showCopilotBadge) View.VISIBLE else View.GONE
        if (showCopilotBadge) {
            if (copilotArrowAnimator?.isStarted != true) copilotArrowAnimator?.start()
        } else copilotArrowAnimator?.cancel()
    }

    private fun reloadAutomation() {
        val supporter = SupporterLicenseManager.load(service) != null
        AutomationState.mode = de.robinthor.digiworldexplorer.automation.AutomationMode.fromPreference(
            preferences.getString("automation_mode", null))
        AutomationState.farmWateringEnabled = preferences.getBoolean("farm_watering", true)
        AutomationState.adSkipPassEnabled = preferences.getBoolean("ad_skip_pass", false)
        de.robinthor.digiworldexplorer.feed.BondRotationAnalyzer.reset()
        AutomationState.overlayEnabled = preferences.getBoolean("grid_enabled", true)
        AutomationState.autoPurchaseEnabled = preferences.getBoolean("auto_purchase", false)
        AutomationState.autoDungeonEnabled = preferences.getBoolean("auto_dungeon", false)
        AutomationState.autoNetworkDefenseEnabled = preferences.getBoolean("auto_network_defense", false)
        AutomationState.autoFeedEnabled = preferences.getBoolean("auto_feed", false)
        AutomationState.autoBondRotationEnabled = supporter && preferences.getBoolean("auto_bond_rotation", true)
        AutomationState.copilotRewardsEnabled = supporter && preferences.getBoolean("copilot_rewards", true)
        AutomationState.copilotDwsEnabled = supporter && preferences.getBoolean("copilot_dws", true)
        AutomationState.autoFarmEnabled = supporter && preferences.getBoolean("auto_farm_harvest", true)
        AutomationState.dwsNavigationSettings =
            de.robinthor.digiworldexplorer.strategy.DwsNavigationProfile.fromPreferenceForAccess(
                preferences.getString("dws_profile", null), supporter).settings()
        RewardPurchaseFrameAnalyzer.reset()
        DungeonFrameAnalyzer.reset()
        NetworkDefenseFrameAnalyzer.reset()
        FeedFrameAnalyzer.reset()
        de.robinthor.digiworldexplorer.farm.FarmHarvestAnalyzer.reset()
        StageFailedFrameAnalyzer.reset()
        CaptureFrameAnalyzer.resetCalibration()
        AutoMoveController.reset()
        service.showStatusOnly(service.getString(R.string.quick_reloaded))
        if (CaptureSessionState.snapshot(AutomationState.enabled).captureActive) {
            AutomationState.enabled = true
            ScreenCaptureService.setAutomation(service, true)
        } else requestCaptureAndStart()
        service.setOverlayEnabled(AutomationState.overlayEnabled)
        refresh()
    }

    private fun openMainApp() {
        service.startActivity(Intent(service, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        })
    }

    private fun requestCaptureAndStart(startMode: String? = null) {
        service.startActivity(Intent(service, CaptureConsentActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startMode?.let { putExtra(CaptureConsentActivity.EXTRA_START_MODE, it) }
        })
    }

    private fun collapse() {
        expanded = false
        panel?.visibility = View.GONE
    }

    private fun installDragAndClick(view: View, layoutParams: WindowManager.LayoutParams,
        onLongPress: (() -> Unit)? = null, onClick: () -> Unit) {
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        var downAt = 0L
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams.x; startY = layoutParams.y
                    downX = event.rawX; downY = event.rawY; downAt = SystemClock.elapsedRealtime(); true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = (startX + event.rawX - downX).toInt().coerceAtLeast(0)
                    layoutParams.y = (startY + event.rawY - downY).toInt().coerceAtLeast(0)
                    root?.let { runCatching { windowManager.updateViewLayout(it, layoutParams) } }
                    moveCopilotBadge(layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = abs(event.rawX - downX) + abs(event.rawY - downY)
                    val clickTolerance = 32f * service.resources.displayMetrics.density
                    val held = SystemClock.elapsedRealtime() - downAt
                    if (moved < clickTolerance) {
                        if (held >= 2_000L && onLongPress != null) onLongPress()
                        else if (held < 1_200L) onClick()
                    }
                    preferences.edit().putInt("quick_overlay_x", layoutParams.x).putInt("quick_overlay_y", layoutParams.y).apply()
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleDirectorCard() {
        setDirectorCardVisible(!preferences.getBoolean("director_card_visible", true))
    }

    private fun togglePanel(layoutParams: WindowManager.LayoutParams) {
        expanded = !expanded
        if (expanded) refreshFeatureSwitches()
        panel?.visibility = if (expanded) View.VISIBLE else View.GONE
        root?.requestLayout()
        root?.let { runCatching { windowManager.updateViewLayout(it, layoutParams) } }
        if (expanded) keepExpandedMenuOnScreen(layoutParams)
        refresh()
    }

    private fun keepExpandedMenuOnScreen(layoutParams: WindowManager.LayoutParams) {
        root?.post {
            val bounds = if (Build.VERSION.SDK_INT >= 30) {
                windowManager.maximumWindowMetrics.bounds
            } else {
                @Suppress("DEPRECATION")
                val metrics = android.util.DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }
                android.graphics.Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
            }
            val width = root?.measuredWidth ?: 0
            val height = root?.measuredHeight ?: 0
            layoutParams.x = layoutParams.x.coerceIn(0, (bounds.width() - width).coerceAtLeast(0))
            layoutParams.y = layoutParams.y.coerceIn(0, (bounds.height() - height).coerceAtLeast(0))
            root?.let { runCatching { windowManager.updateViewLayout(it, layoutParams) } }
            moveCopilotBadge(layoutParams)
            preferences.edit().putInt("quick_overlay_x", layoutParams.x).putInt("quick_overlay_y", layoutParams.y).apply()
        }
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = radius
        setStroke(1, Color.rgb(52, 57, 65))
    }
}
