package com.focuscrew.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focuscrew.app.ui.blocker.CalmBlockActivity
import com.focuscrew.app.ui.reel.TranceBreakerActivity

class BlockerAccessibilityService : AccessibilityService() {

    private var lastBlockedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L

    /**
     * Tracks the target reel app ONLY after a genuine reel scroll is detected.
     * null = user is NOT in a reels feed. Set only by confirmed reel swipe events.
     */
    private var currentTargetReelApp: String? = null
    private var isOverlayCurrentlyVisible = false

    /** Real-time foreground app time tracking for Instagram and YouTube */
    private var currentForegroundPackage: String? = null
    private var foregroundAppStartTime: Long = 0L

    /** Remembers active browser so Gboard/system keyboard typing is scanned */
    private var activeBrowserPackage: String? = null

    /** Active reel session watch heartbeat tracking */
    private var activeReelApp: String? = null
    private var lastWatchHeartbeatMs: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val watchHeartbeatRunnable = object : Runnable {
        override fun run() {
            val pkg = activeReelApp
            if (pkg != null) {
                val now = System.currentTimeMillis()
                if (lastWatchHeartbeatMs > 0L) {
                    val delta = now - lastWatchHeartbeatMs
                    if (delta in 500L..5000L) {
                        ReelCounterManager.pulseActiveWatchTime(pkg, delta)
                    }
                }
                lastWatchHeartbeatMs = now
                mainHandler.postDelayed(this, 1000L)
            } else {
                lastWatchHeartbeatMs = 0L
            }
        }
    }

    /** Auto-hides overlay after 3.5s of no reel scroll activity */
    private val hideOverlayRunnable = Runnable {
        currentTargetReelApp = null
        if (isOverlayCurrentlyVisible) {
            ReelOverlayManager.hide()
            isOverlayCurrentlyVisible = false
            Log.d(TAG, "Auto-hide: overlay dismissed after 3.5s of no reel scrolling")
        }
    }

    // System daemons, keyboards, and overlays that must NEVER trigger overlay hiding
    private val transientSystemPackages = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "android",
        "com.google.android.as",
        "com.google.android.gms",
        "com.motorola.gestures",
        "com.motorola.gamemode",
        "com.motorola.motocare",
        "com.motorola.securevault",
        "com.motorola.smartfeed",
        "com.motorola.freeform",
        "com.motorola.systemui",
        "com.motorola.ccc.notification",
        "com.motorola.screenshottalk",
        "com.google.android.permissioncontroller"
    )

    // Critical system packages that must NEVER be blocked
    private val essentialSafePackages = setOf(
        "com.focuscrew.app",
        "com.android.settings",
        "com.android.systemui",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.phone",
        "com.google.android.emergency",
        "com.android.emergency"
    )

    // Social media, chat, and messaging apps that must NEVER have chats inspected or blocked
    // Anti-Porn Defender is strictly restricted to web browsers only (Chrome, Edge, Firefox, Brave, etc.)
    private val excludedSocialAndMessagingPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.instagram.android",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.snapchat.android",
        "org.telegram.messenger",
        "org.telegram.plus",
        "com.twitter.android",
        "com.x.android",
        "com.discord",
        "com.viber.voip",
        "com.tencent.mm",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.google.android.talk",
        "com.skype.raider"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "BlockerAccessibilityService connected")
        BlockerManager.setServiceConnected(true)
        BlockProfileManager.initialize(applicationContext)
        ReelCounterManager.initialize(applicationContext)
        FreedomVaultManager.initialize(applicationContext)
        AntiPornDefenderManager.initialize(applicationContext)
        AntiPornDefenderManager.setDefenderEnabled(true)
        AntiPornDefenderManager.setSafeSearchEnforced(true)
        FocusCrewVpnService.initialize(applicationContext)
        ReelOverlayManager.destroy()
        ReelNotificationManager.createNotificationChannel(applicationContext)
    }

    private fun startWatchHeartbeat(pkg: String) {
        activeReelApp = pkg
        lastWatchHeartbeatMs = System.currentTimeMillis()
        mainHandler.removeCallbacks(watchHeartbeatRunnable)
        mainHandler.postDelayed(watchHeartbeatRunnable, 1000L)
    }

    private fun stopWatchHeartbeat() {
        val pkg = activeReelApp
        if (pkg != null && lastWatchHeartbeatMs > 0L) {
            val now = System.currentTimeMillis()
            val delta = now - lastWatchHeartbeatMs
            if (delta in 500L..5000L) {
                ReelCounterManager.pulseActiveWatchTime(pkg, delta)
            }
        }
        activeReelApp = null
        lastWatchHeartbeatMs = 0L
        mainHandler.removeCallbacks(watchHeartbeatRunnable)
    }

    private fun endReelSessionIfActive() {
        stopWatchHeartbeat()
        ReelCounterManager.endReelSession()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val rawPackageName = event.packageName?.toString() ?: ""

        val isInstagram = rawPackageName.contains("instagram")
        val isYoutube = rawPackageName == "com.google.android.youtube" || rawPackageName.contains("youtube")
        val isTargetReelApp = isInstagram || isYoutube

        // ==========================================
        // 1. Reel & Shorts Scroll Gesture Interception
        // ONLY TYPE_VIEW_SCROLLED from target reel apps
        // ==========================================
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            // Ignore FocusCrew's own scroll events
            if (rawPackageName == packageName) return

            // Fallback to active foreground package if event didn't populate package name
            val effectivePkg = if (rawPackageName.isNotEmpty()) rawPackageName else (currentForegroundPackage ?: "")
            val isIg = effectivePkg.contains("instagram")
            val isYt = effectivePkg == "com.google.android.youtube" || effectivePkg.contains("youtube")

            if (isIg || isYt) {

            val activePkg = if (isIg) "com.instagram.android" else "com.google.android.youtube"
            val rawDeltaY = event.scrollDeltaY
            val rawDeltaX = event.scrollDeltaX
            val deltaY = if (rawDeltaY > 0) rawDeltaY else 0
            val deltaX = if (rawDeltaX > 0) rawDeltaX else 0
            val className = event.className?.toString() ?: ""

            val sourceNode = try { event.source } catch (e: Exception) { null }
            val viewId = sourceNode?.viewIdResourceName ?: ""

            // High-precision Instagram Chat / DM Isolation
            var isChatOrDm = false
            var isClipsContext = true

            if (isIg) {
                val lowerViewId = viewId.lowercase()

                // 1. Direct signature check on source node
                if (lowerViewId.contains("message") ||
                    lowerViewId.contains("direct") ||
                    lowerViewId.contains("thread") ||
                    lowerViewId.contains("chat") ||
                    lowerViewId.contains("inbox") ||
                    lowerViewId.contains("composer")
                ) {
                    isChatOrDm = true
                    isClipsContext = false
                }

                // 2. Parent chain inspection (up to 5 levels)
                var parent = sourceNode?.parent
                var depth = 0
                while (parent != null && depth < 5) {
                    val pId = parent.viewIdResourceName?.lowercase() ?: ""
                    if (pId.contains("message") ||
                        pId.contains("direct") ||
                        pId.contains("thread") ||
                        pId.contains("chat") ||
                        pId.contains("composer")
                    ) {
                        isChatOrDm = true
                        isClipsContext = false
                        break
                    }
                    parent = parent.parent
                    depth++
                }

                // 3. Window hierarchy inspection for direct chat composer
                val root = try { rootInActiveWindow } catch (e: Exception) { null }
                if (root != null) {
                    val hasDmElements = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/row_thread_composer_edittext")?.isNotEmpty() == true ||
                                        root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/direct_text_message_text_view")?.isNotEmpty() == true ||
                                        root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/message_list")?.isNotEmpty() == true

                    if (hasDmElements) {
                        isChatOrDm = true
                        isClipsContext = false
                    }
                }

                // 4. Final safety guard: any chat/DM detection strictly disables clips
                if (isChatOrDm) {
                    isClipsContext = false
                }
            }

            // Pass packageName, className, viewId, and context flags to classifier
            val isGenuine = com.focuscrew.app.domain.ReelSwipeClassifier.isGenuineReelSwipe(
                packageName = activePkg,
                deltaY = deltaY,
                deltaX = deltaX,
                fromIndex = event.fromIndex,
                toIndex = event.toIndex,
                className = className,
                viewId = viewId,
                isChatOrDmContext = isChatOrDm,
                isClipsContext = isClipsContext
            )

            if (!isGenuine) {
                // If user is in DMs or non-reel area, ensure overlay stays hidden and stop tracking reel time
                if (isChatOrDm || !isClipsContext) {
                    ReelOverlayManager.hide()
                    isOverlayCurrentlyVisible = false
                    endReelSessionIfActive()
                }
                return
            }

            // At this point we have a CONFIRMED genuine reel swipe from Instagram or YouTube
            currentTargetReelApp = activePkg
            currentForegroundPackage = activePkg
            startWatchHeartbeat(activePkg)

            // Cancel any pending hide — we just got a fresh reel swipe
            cancelPendingHideOverlay()

            val swiped = ReelCounterManager.recordSwipe(activePkg, event.fromIndex)
            if (swiped) {
                val stats = ReelCounterManager.stats.value
                if (!isOverlayCurrentlyVisible && stats.isOverlayEnabled) {
                    ReelOverlayManager.show(this, stats)
                    isOverlayCurrentlyVisible = true
                } else {
                    ReelOverlayManager.update(stats)
                }
                ReelNotificationManager.updateNotification(applicationContext, stats)
                Log.d(TAG, "Counted Reel swipe! Total today: ${stats.todayTotal} ($activePkg, class=$className)")
            }

            // Schedule auto-hide after 3.5s of no more reel scrolling
            scheduleAutoHideOverlay()
            return
        }
    }

        // ==========================================
        // 2. Anti-Porn Defender: Browser Search & Site Interception
        // STRICT RULE: ONLY runs when the user is ACTUALLY inside a supported web browser!
        // Social media platforms, games, video editors, and messaging apps are 100% EXEMPT and NEVER blocked!
        // ==========================================
        val isCurrentEventBrowser = AntiPornDefenderManager.isBrowserPackage(rawPackageName)
        val isKeyboardEvent = rawPackageName == "com.google.android.inputmethod.latin" || rawPackageName.contains("inputmethod")

        // If user is in ANY non-browser app (and not typing on keyboard), immediately clear activeBrowserPackage!
        if (!isCurrentEventBrowser && !isKeyboardEvent && rawPackageName.isNotEmpty()) {
            activeBrowserPackage = null
        }

        if (isCurrentEventBrowser) {
            currentForegroundPackage = rawPackageName
            activeBrowserPackage = rawPackageName
        }

        // Strictly determine if the user is currently inside a browser
        val effectiveBrowserPkg: String? = when {
            isCurrentEventBrowser -> rawPackageName
            isKeyboardEvent && activeBrowserPackage != null -> activeBrowserPackage
            else -> null // Absolutely NEVER fallback to non-browser apps or background windows!
        }

        val isStrictlyInBrowser = effectiveBrowserPkg != null && AntiPornDefenderManager.isBrowserPackage(effectiveBrowserPkg)

        if (AntiPornDefenderManager.isDefenderEnabled.value &&
            isStrictlyInBrowser &&
            !isEssentialSafePackage(rawPackageName) &&
            rawPackageName != packageName
        ) {
            val effectivePkg = effectiveBrowserPkg!!
            var detectedTrigger: String? = null

            // 1. Immediate Event Text Check (catches text being typed, search queries, URL bar text)
            for (textItem in event.text) {
                val str = textItem?.toString() ?: ""
                if (str.isNotBlank()) {
                    val t = AntiPornDefenderManager.detectAdultContent(str)
                    if (t != null) {
                        detectedTrigger = t
                        break
                    }
                }
            }

            // 2. Event Source Text Check
            if (detectedTrigger == null) {
                val sourceText = event.source?.text?.toString() ?: ""
                if (sourceText.isNotBlank()) {
                    val t = AntiPornDefenderManager.detectAdultContent(sourceText)
                    if (t != null) detectedTrigger = t
                }
            }

            // 3. Event Content Description Check
            if (detectedTrigger == null) {
                val desc = event.contentDescription?.toString() ?: ""
                if (desc.isNotBlank()) {
                    val t = AntiPornDefenderManager.detectAdultContent(desc)
                    if (t != null) detectedTrigger = t
                }
            }

            // 4. SafeSearch & DNS Tamper Prevention: If user tries to disable SafeSearch or change DNS
            if (detectedTrigger == null && AntiPornDefenderManager.isSafeSearchEnforced.value) {
                val combinedText = buildString {
                    for (t in event.text) append(t?.toString()?.lowercase() ?: "").append(' ')
                    append(event.contentDescription?.toString()?.lowercase() ?: "").append(' ')
                    append(event.source?.text?.toString()?.lowercase() ?: "")
                }
                if (isSafeSearchTamper(combinedText)) {
                    detectedTrigger = "SafeSearch Lock: SafeSearch is permanently locked on!"
                } else if (isDnsTamperAttempt(combinedText)) {
                    detectedTrigger = "DNS Shield Lock: Secure DNS cannot be changed to bypass protection!"
                }
            }

            // 5. Deep Window Hierarchy Check
            if (detectedTrigger == null) {
                val browserRoot = getActiveBrowserRootNode()
                detectedTrigger = AntiPornDefenderManager.scanBrowserForAdultContent(browserRoot)
            }

            // 6. Incognito / Private Tab Interception (Zero-Leak Habit Guard)
            if (detectedTrigger == null && FreedomVaultManager.state.value.blockIncognitoEnabled) {
                val combinedText = buildString {
                    for (t in event.text) append(t?.toString()?.lowercase() ?: "").append(' ')
                    append(event.contentDescription?.toString()?.lowercase() ?: "").append(' ')
                    append(event.source?.text?.toString()?.lowercase() ?: "")
                }
                if (combinedText.contains("incognito") ||
                    combinedText.contains("private tab") ||
                    combinedText.contains("inprivate") ||
                    combinedText.contains("secret mode")
                ) {
                    detectedTrigger = "Incognito Mode Blocked"
                }
            }

            if (detectedTrigger != null) {
                val now = System.currentTimeMillis()
                if (effectivePkg != lastBlockedPackage || (now - lastBlockedTimestamp) > 1000L) {
                    lastBlockedPackage = effectivePkg
                    lastBlockedTimestamp = now
                    AntiPornDefenderManager.recordSearchBlocked()
                    Log.d(TAG, "Anti-Porn Defender intercepted: $effectivePkg with trigger \"$detectedTrigger\"")


                    val blockType = if (detectedTrigger.contains("SafeSearch")) {
                        CalmBlockActivity.BLOCK_TYPE_SAFESEARCH
                    } else {
                        CalmBlockActivity.BLOCK_TYPE_ADULT_CONTENT
                    }

                    val blockIntent = Intent(this, CalmBlockActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(CalmBlockActivity.EXTRA_BLOCKED_PACKAGE, effectivePkg)
                        putExtra(CalmBlockActivity.EXTRA_BLOCKED_LABEL, detectedTrigger)
                        putExtra(CalmBlockActivity.EXTRA_BLOCK_TYPE, blockType)
                    }
                    startActivity(blockIntent)
                    return
                }
            }
        }

        // ==========================================
        // 3. Foreground Window State Change
        // ==========================================
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (rawPackageName.isEmpty()) return

            // Ignore transient system overlays (keyboard, volume HUD, etc.)
            if (transientSystemPackages.contains(rawPackageName)) {
                return
            }

            val now = System.currentTimeMillis()

            // When returning to FocusCrew, hide overlay and end reel session
            if (rawPackageName == packageName) {
                endReelSessionIfActive()
                currentForegroundPackage = null
                foregroundAppStartTime = 0L
                if (isOverlayCurrentlyVisible) {
                    scheduleGracefulHideOverlay()
                }
                currentTargetReelApp = null
                return
            }

            if (isTargetReelApp) {
                val targetPkg = if (isInstagram) "com.instagram.android" else "com.google.android.youtube"
                if (currentForegroundPackage != targetPkg) {
                    endReelSessionIfActive()
                    currentForegroundPackage = targetPkg
                    foregroundAppStartTime = now
                }
                // Keep overlay alive if already active
                if (isOverlayCurrentlyVisible) {
                    cancelPendingHideOverlay()
                    scheduleAutoHideOverlay()
                }
            } else {
                // Switched away to a different app
                endReelSessionIfActive()
                currentForegroundPackage = rawPackageName
                foregroundAppStartTime = 0L

                if (excludedSocialAndMessagingPackages.contains(rawPackageName)) {
                    activeBrowserPackage = null
                } else if (AntiPornDefenderManager.isBrowserPackage(rawPackageName)) {
                    activeBrowserPackage = rawPackageName
                } else if (!transientSystemPackages.contains(rawPackageName)) {
                    activeBrowserPackage = null
                }

                if (isOverlayCurrentlyVisible) {
                    scheduleGracefulHideOverlay()
                }
                currentTargetReelApp = null
            }

            // 3. Never block our own app or essential phone utilities
            if (isEssentialSafePackage(rawPackageName)) {
                if (rawPackageName == "com.android.settings") {
                    val combinedText = buildString {
                        for (t in event.text) append(t?.toString()?.lowercase() ?: "").append(' ')
                        append(event.contentDescription?.toString()?.lowercase() ?: "").append(' ')
                        append(event.source?.text?.toString()?.lowercase() ?: "")
                    }
                    if (isDnsTamperAttempt(combinedText)) {
                        Log.d(TAG, "🔒 Intercepted Private DNS settings attempt in Android Settings")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        android.widget.Toast.makeText(applicationContext, "🔒 FocusCrew: Private DNS settings are protected by Focus Shield", android.widget.Toast.LENGTH_SHORT).show()
                    }

                    // Anti-Tamper: Protect FocusCrew from Force-Stop or Uninstall in Android Settings
                    val isSessionActive = com.focuscrew.app.ui.timer.TimerManager.snapshot.value.remainingMs > 0 || FreedomVaultManager.isCurrentTimeInDangerZone()
                    if (AppSecurityManager.isSettingsAntiTamperActive.value && isSessionActive) {
                        if (combinedText.contains("focuscrew") || combinedText.contains("focus crew")) {
                            Log.d(TAG, "🔒 Intercepted FocusCrew App Info attempt in Android Settings during active focus")
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            android.widget.Toast.makeText(
                                applicationContext,
                                "🔒 FocusCrew settings are locked during active focus to protect your streak.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                return
            }

            // 4. Check if the app is currently in the active block list OR blocked by Freedom Vault Danger Zone
            val isFreedomVaultCurfew = FreedomVaultManager.shouldInterceptPackage(rawPackageName)
            val isTimerBlocked = BlockerManager.isPackageBlocked(rawPackageName)

            if (isTimerBlocked || isFreedomVaultCurfew) {
                if (rawPackageName == lastBlockedPackage && (now - lastBlockedTimestamp) < 1500L) {
                    return
                }

                lastBlockedPackage = rawPackageName
                lastBlockedTimestamp = now

                val appLabel = getAppLabel(rawPackageName)
                val displayLabel = if (isFreedomVaultCurfew && !isTimerBlocked) {
                    "$appLabel (Danger Zone Curfew)"
                } else {
                    appLabel
                }
                Log.d(TAG, "Intercepting blocked app: $rawPackageName ($displayLabel)")

                val blockIntent = Intent(this, CalmBlockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(CalmBlockActivity.EXTRA_BLOCKED_PACKAGE, rawPackageName)
                    putExtra(CalmBlockActivity.EXTRA_BLOCKED_LABEL, displayLabel)
                }
                startActivity(blockIntent)
            }
        }
    }

    private fun cancelPendingHideOverlay() {
        mainHandler.removeCallbacks(hideOverlayRunnable)
    }

    /** Hide overlay quickly (500ms) when user switches to a different app entirely */
    private fun scheduleGracefulHideOverlay() {
        mainHandler.removeCallbacks(hideOverlayRunnable)
        mainHandler.postDelayed(hideOverlayRunnable, 500L)
    }

    /** Auto-hide overlay after 3.5s of no reel scroll activity */
    private fun scheduleAutoHideOverlay() {
        mainHandler.removeCallbacks(hideOverlayRunnable)
        mainHandler.postDelayed(hideOverlayRunnable, 3500L)
    }

    override fun onInterrupt() {
        Log.d(TAG, "BlockerAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BlockerAccessibilityService destroyed")
        endReelSessionIfActive()
        currentForegroundPackage = null
        foregroundAppStartTime = 0L
        cancelPendingHideOverlay()
        BlockerManager.setServiceConnected(false)
        ReelOverlayManager.destroy()
        isOverlayCurrentlyVisible = false
    }


    private fun isEssentialSafePackage(pkg: String): Boolean {
        if (essentialSafePackages.contains(pkg)) return true
        return isHomeScreenLauncher(pkg)
    }

    private fun isHomeScreenLauncher(pkg: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            resolveInfo?.activityInfo?.packageName == pkg
        } catch (e: Exception) {
            false
        }
    }

    private fun getAppLabel(pkg: String): String {
        return try {
            val appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(pkg, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun isSafeSearchTamper(text: String): Boolean {
        val lower = text.lowercase()
        if (lower.contains("google.com/safesearch") ||
            lower.contains("google.com/preferences") ||
            lower.contains("safe=off") ||
            lower.contains("safe=images") ||
            lower.contains("safesearch=0") ||
            lower.contains("safesearch=off") ||
            lower.contains("adlt=off")
        ) {
            return true
        }

        // Only trigger on explicit intent to turn off SafeSearch, NEVER on Google's own "SafeSearch: Filter" or "Manage SafeSearch" labels!
        if (lower.contains("turn off safesearch") ||
            lower.contains("turn safesearch off") ||
            lower.contains("disable safesearch") ||
            lower.contains("disable safe search") ||
            lower.contains("turn off safe search")
        ) {
            return true
        }

        return false
    }

    private fun isDnsTamperAttempt(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("use secure dns") ||
               lower.contains("secure dns") ||
               lower.contains("choose another provider") ||
               lower.contains("custom dns") ||
               lower.contains("private dns") ||
               lower.contains("dns provider hostname") ||
               lower.contains("cloudflare-dns") ||
               lower.contains("dns.google") ||
               lower.contains("nextdns") ||
               lower.contains("cleanbrowsing") ||
               lower.contains("adguard-dns")
    }

    private fun findActiveBrowserFromWindows(): String? {
        try {
            val windowList = windows
            if (!windowList.isNullOrEmpty()) {
                for (w in windowList) {
                    val root = w.root ?: continue
                    val pkg = root.packageName?.toString() ?: ""
                    if (AntiPornDefenderManager.isBrowserPackage(pkg)) {
                        return pkg
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun getActiveBrowserRootNode(): AccessibilityNodeInfo? {
        val root = try { rootInActiveWindow } catch (e: Exception) { null }
        if (root != null && AntiPornDefenderManager.isBrowserPackage(root.packageName?.toString() ?: "")) {
            return root
        }
        try {
            val windowList = windows
            if (!windowList.isNullOrEmpty()) {
                for (w in windowList) {
                    val wRoot = w.root ?: continue
                    val pkg = wRoot.packageName?.toString() ?: ""
                    if (AntiPornDefenderManager.isBrowserPackage(pkg)) {
                        return wRoot
                    }
                }
            }
        } catch (_: Exception) {}
        return root
    }

    companion object {
        private const val TAG = "FocusCrewBlocker"
    }
}
