package com.focuscrew.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.focuscrew.app.R
import com.focuscrew.app.data.model.BrainHealthState
import com.focuscrew.app.data.model.ReelStats
import com.focuscrew.app.ui.MainActivity
import com.focuscrew.app.ui.reel.TranceBreakerActivity

/**
 * Dynamic Evolving Brainrot Capsule HUD rendered under the camera punch-hole
 * while the user is actively browsing Instagram Reels or YouTube Shorts.
 * 
 * Features:
 * - 5-Stage Evolutionary Brain Mascot (Fresh -> Concerned -> Weary -> Exhausted -> Full Brainrot)
 * - Warm high-contrast amber typography matching reference design
 * - Smooth micro-bounce & wobble animations on each counted reel swipe
 * - Single-tap expansion to reveal health diagnosis / limit, double-tap to open FocusCrew
 * - Anti-flicker architecture with stable WindowManager attachment
 */
object ReelOverlayManager {

    private var windowManager: WindowManager? = null
    private var overlayView: LinearLayout? = null
    private var brainImageView: ImageView? = null
    private var pillTextView: TextView? = null
    private var statusDetailTextView: TextView? = null
    private var isViewAttached = false

    private var lastCount: Int = -1
    private var lastBrainStage: BrainHealthState? = null
    private var isDetailExpanded = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var collapseRunnable: Runnable? = null

    fun show(context: Context, stats: ReelStats) {
        if (!stats.isOverlayEnabled) {
            hide()
            return
        }

        // If view is attached, make visible and update
        if (isViewAttached && overlayView != null && overlayView?.isAttachedToWindow == true) {
            overlayView?.visibility = View.VISIBLE
            update(stats)
            return
        }

        if (isViewAttached || overlayView != null) {
            destroy()
        }

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

            val isAccessibilityContext = context is AccessibilityService
            val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isAccessibilityContext || Settings.canDrawOverlays(context)
            } else {
                true
            }

            if (!canDraw) return

            val windowType = if (isAccessibilityContext) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = context.resources.displayMetrics.density
            val yOffsetPx = (28 * density).toInt()

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = yOffsetPx
            }

            val pillLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((8 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                isFocusable = false
                isFocusableInTouchMode = false

                // Interactive tap behavior:
                // Tap expands status detail quote for 3 seconds. Double tap or tap on expanded opens FocusCrew!
                setOnClickListener {
                    if (!isDetailExpanded) {
                        expandDetail(context, stats)
                    } else {
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("navigate_to", "REEL_COUNTER")
                        }
                        context.startActivity(intent)
                    }
                }

                // Long-press pill launches Trance-Breaker reality check directly
                setOnLongClickListener {
                    TranceBreakerActivity.launch(context, triggeredByMilestone = false)
                    true
                }
            }

            // 1. Dynamic Brain Mascot View (28x28dp)
            val brainSize = (28 * density).toInt()
            val brainParams = LinearLayout.LayoutParams(brainSize, brainSize).apply {
                setMargins(0, 0, (8 * density).toInt(), 0)
                gravity = Gravity.CENTER_VERTICAL
            }
            val brainView = ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setImageResource(getBrainDrawableRes(stats.brainState))
                // 1-Tap on Brain Mascot immediately launches Trance-Breaker modal
                setOnClickListener {
                    TranceBreakerActivity.launch(context, triggeredByMilestone = false)
                }
            }
            pillLayout.addView(brainView, brainParams)
            brainImageView = brainView

            // 2. Bold Clean Reel Count Number
            val countTextView = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                text = "${stats.todayTotal}"
                setTextColor(getCountTextColor(stats.brainState))
            }
            pillLayout.addView(countTextView)
            pillTextView = countTextView

            // 3. Expandable Subtitle / Diagnosis (Hidden by default, reveals on tap)
            val detailTextView = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(Color.parseColor("#E0D6CD"))
                visibility = View.GONE
                setPadding((6 * density).toInt(), 0, 0, 0)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            pillLayout.addView(detailTextView)
            statusDetailTextView = detailTextView

            applyPillBackground(pillLayout, stats.brainState, density)

            windowManager?.addView(pillLayout, layoutParams)
            overlayView = pillLayout
            isViewAttached = true
            lastCount = stats.todayTotal
            lastBrainStage = stats.brainState
        } catch (e: Exception) {
            isViewAttached = false
            overlayView = null
        }
    }

    fun update(stats: ReelStats) {
        if (!isViewAttached || overlayView == null) return
        try {
            val pill = overlayView ?: return
            val density = pill.context.resources.displayMetrics.density

            pill.visibility = if (stats.isOverlayEnabled) View.VISIBLE else View.GONE
            pillTextView?.text = "${stats.todayTotal}"
            pillTextView?.setTextColor(getCountTextColor(stats.brainState))

            // Update brain state asset and background when state transitions
            if (lastBrainStage != stats.brainState) {
                brainImageView?.setImageResource(getBrainDrawableRes(stats.brainState))
                applyPillBackground(pill, stats.brainState, density)
                lastBrainStage = stats.brainState
            }

            // Tactile feedback: Micro-bounce & wobble whenever a new reel is counted
            if (lastCount != -1 && stats.todayTotal > lastCount) {
                triggerScrollMicroAnimation(pill, brainImageView, pillTextView)
            }
            lastCount = stats.todayTotal

            if (isDetailExpanded) {
                statusDetailTextView?.text = "• ${stats.brainState.title}"
            }
        } catch (e: Exception) {
            // Protect against UI hitches
        }
    }

    private fun expandDetail(context: Context, stats: ReelStats) {
        isDetailExpanded = true
        statusDetailTextView?.apply {
            text = "• ${stats.brainState.title} (${stats.todayTotal}/${stats.dailyLimit})"
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(180).start()
        }

        collapseRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            collapseDetail()
        }
        collapseRunnable = runnable
        mainHandler.postDelayed(runnable, 3200)
    }

    private fun collapseDetail() {
        isDetailExpanded = false
        statusDetailTextView?.animate()
            ?.alpha(0f)
            ?.setDuration(180)
            ?.withEndAction {
                statusDetailTextView?.visibility = View.GONE
            }
            ?.start()
    }

    private fun triggerScrollMicroAnimation(pill: View, brain: View?, countText: View?) {
        // Number bounce pop
        countText?.animate()
            ?.scaleX(1.25f)
            ?.scaleY(1.25f)
            ?.setDuration(100)
            ?.withEndAction {
                countText.animate()
                    ?.scaleX(1.0f)
                    ?.scaleY(1.0f)
                    ?.setInterpolator(OvershootInterpolator(2.5f))
                    ?.setDuration(160)
                    ?.start()
            }
            ?.start()

        // Brain mascot playful wobble
        brain?.animate()
            ?.rotation(14f)
            ?.setDuration(75)
            ?.withEndAction {
                brain.animate()
                    ?.rotation(-12f)
                    ?.setDuration(75)
                    ?.withEndAction {
                        brain.animate()
                            ?.rotation(0f)
                            ?.setDuration(75)
                            ?.start()
                    }
                    ?.start()
            }
            ?.start()
    }

    private fun applyPillBackground(view: View, stage: BrainHealthState, density: Float) {
        val strokeColor = when (stage) {
            BrainHealthState.FRESH -> Color.parseColor("#38291F")      // Subtle bronze obsidian
            BrainHealthState.CONCERNED -> Color.parseColor("#523A25")   // Warm bronze
            BrainHealthState.WEARY -> Color.parseColor("#6B4226")       // Amber ochre
            BrainHealthState.EXHAUSTED -> Color.parseColor("#7D2828")   // Warning crimson
            BrainHealthState.BRAINROT -> Color.parseColor("#B02525")    // Hot emergency red
        }

        val fillColor = when (stage) {
            BrainHealthState.BRAINROT -> Color.parseColor("#F51C1210")  // Darker danger tinted obsidian
            BrainHealthState.EXHAUSTED -> Color.parseColor("#F21B1310")
            else -> Color.parseColor("#F0191310")                       // Deep warm espresso obsidian
        }

        val pillBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24 * density
            setColor(fillColor)
            setStroke((1.4f * density).toInt(), strokeColor)
        }
        view.background = pillBackground
    }

    private fun getCountTextColor(stage: BrainHealthState): Int {
        return when (stage) {
            BrainHealthState.FRESH, BrainHealthState.CONCERNED -> Color.parseColor("#FFA94D") // Warm amber
            BrainHealthState.WEARY -> Color.parseColor("#FF922B")                              // Deep amber
            BrainHealthState.EXHAUSTED -> Color.parseColor("#FA5252")                          // Alert coral
            BrainHealthState.BRAINROT -> Color.parseColor("#FF6B6B")                           // Neon alert red
        }
    }

    private fun getBrainDrawableRes(stage: BrainHealthState): Int {
        return when (stage) {
            BrainHealthState.FRESH -> R.drawable.ic_brain_state_0
            BrainHealthState.CONCERNED -> R.drawable.ic_brain_state_1
            BrainHealthState.WEARY -> R.drawable.ic_brain_state_2
            BrainHealthState.EXHAUSTED -> R.drawable.ic_brain_state_3
            BrainHealthState.BRAINROT -> R.drawable.ic_brain_state_4
        }
    }

    fun hide() {
        if (!isViewAttached || overlayView == null) return
        try {
            overlayView?.visibility = View.GONE
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun destroy() {
        collapseRunnable?.let { mainHandler.removeCallbacks(it) }
        if (!isViewAttached || overlayView == null) return
        try {
            windowManager?.removeView(overlayView)
        } catch (e: Exception) {
            // View may already be detached
        } finally {
            overlayView = null
            brainImageView = null
            pillTextView = null
            statusDetailTextView = null
            isViewAttached = false
            lastCount = -1
            lastBrainStage = null
            isDetailExpanded = false
        }
    }
}
