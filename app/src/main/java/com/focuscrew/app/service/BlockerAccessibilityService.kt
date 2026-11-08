package com.focuscrew.app.service

/**
 * 🔒 FROZEN MODULE — VERIFIED & LOCKED (v1.0.0-stable)
 * DO NOT MODIFY THIS FILE WITHOUT EXPLICIT OPERATOR APPROVAL.
 * Core accessibility blocker: SafeSearch settings detection/exit, adult query clearing, and reel gesture detection.
 */

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.focuscrew.app.ui.blocker.CalmBlockActivity
import com.focuscrew.app.ui.reel.TranceBreakerActivity

class BlockerAccessibilityService : AccessibilityService() {

    private var lastBlockedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L
    private var lastSafeSearchExitTimestamp: Long = 0L

    /**
     * Tracks the target reel app ONLY after a genuine reel scroll is detected.
     * null = user is NOT in a reels feed. Set only by confirmed reel swipe events.
     */
    private var currentTargetReelApp: String? = null
    private var isOverlayCurrentlyVisible = false

    /** Real-time foreground app time tracking for Instagram and YouTube */
    private var currentForegroundPackage: String? = null
    private var foregroundAppStartTime: Long = 0L


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
        BlockerManager.initialize(applicationContext)
        BlockerManager.setServiceConnected(true)
        BlockProfileManager.initialize(applicationContext)
        ReelCounterManager.initialize(applicationContext)
        FreedomVaultManager.initialize(applicationContext)
        AntiPornDefenderManager.initialize(applicationContext)
        AntiPornDefenderManager.setDefenderEnabled(true)
        AntiPornDefenderManager.setSafeSearchEnforced(true)
        FocusCrewVpnService.initialize(applicationContext)
        FocusCrewVpnService.autoStartIfPermitted(applicationContext)
        ReelOverlayManager.destroy()
        ReelNotificationManager.createNotificationChannel(applicationContext)
        com.focuscrew.app.util.UsageStatsHelper.syncExactTimesIfPermitted(applicationContext)
    }

    private fun flushForegroundTime(now: Long = System.currentTimeMillis()) {
        val pkg = currentForegroundPackage
        if (pkg != null && (pkg.contains("instagram") || pkg.contains("youtube")) && foregroundAppStartTime > 0L) {
            val delta = now - foregroundAppStartTime
            if (delta in 500L..(12 * 3600 * 1000L)) {
                val targetPkg = if (pkg.contains("instagram")) "com.instagram.android" else "com.google.android.youtube"
                ReelCounterManager.addTimeSpent(targetPkg, delta)
            }
        }
        foregroundAppStartTime = now
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
        flushForegroundTime()
        stopWatchHeartbeat()
        ReelCounterManager.endReelSession()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val rawPackageName = event.packageName?.toString() ?: ""

        // Keep currentForegroundPackage updated for non-transient packages
        if (rawPackageName.isNotEmpty() && !transientSystemPackages.contains(rawPackageName)) {
            currentForegroundPackage = rawPackageName
        }

        // ==========================================
        // 0. High-Priority Universal App Blocker Check
        // Intercepts blocked apps on any event (window change, scroll, click, content change)
        // ==========================================
        val effectiveAppPkg = if (rawPackageName.isNotEmpty() && !transientSystemPackages.contains(rawPackageName)) {
            rawPackageName
        } else {
            currentForegroundPackage ?: ""
        }

        if (effectiveAppPkg.isNotEmpty() && !isEssentialSafePackage(effectiveAppPkg) && effectiveAppPkg != packageName) {
            val isCurfew = FreedomVaultManager.shouldInterceptPackage(effectiveAppPkg)
            val isTimerBlocked = BlockerManager.isPackageBlocked(effectiveAppPkg)
            if (isCurfew || isTimerBlocked) {
                enforceAppBlock(effectiveAppPkg, isCurfew, isTimerBlocked)
                return
            }
        }

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
        // ==========================================
        // 2. Layer 1: Anti-Porn Defender (Browser Search & URL Shield)
        // STRICT RULE: ONLY runs when the user is ACTUALLY inside a supported web browser!
        // All messaging apps, social platforms, games, and system tools are 100% EXEMPT and NEVER blocked!
        // ==========================================
        val activeBrowser = resolveActiveBrowserPackage(rawPackageName)

        if (activeBrowser != null) {
            FocusCrewVpnService.autoStartIfPermitted(applicationContext)
        }

        // ── PROACTIVE SAFESEARCH FILTER LOCK & BLOCKER (NUCLEAR) ──
        // This runs on EVERY event in a browser or search app to enforce FILTER mode!
        if (activeBrowser != null && AntiPornDefenderManager.isSafeSearchEnforced.value) {
            val browserRoot = getActiveBrowserRootNode(event)
            val activeUrl = AntiPornDefenderManager.extractActiveBrowserUrl(browserRoot)

            // CRITICAL: Localhost, internal test servers, history, and whitelisted non-search platforms are 100% exempt!
            val isWhitelistedNonSearch = activeUrl != null &&
                AntiPornDefenderManager.isCleanOrWhitelistedUrl(activeUrl) &&
                !AntiPornDefenderManager.isSearchDomainOrEngine(activeUrl, browserRoot)

            if ((activeUrl != null && AntiPornDefenderManager.isLocalOrExemptUrl(activeUrl)) ||
                AntiPornDefenderManager.isBrowserHistoryScreen(activeUrl, browserRoot) ||
                isWhitelistedNonSearch
            ) {
                return
            }

            // 1. Intercept user actively tapping "Off" or "Blur" in SafeSearch
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                if (AntiPornDefenderManager.isAttemptingToDisableSafeSearch(event, browserRoot)) {
                    Log.d(TAG, "🔒 BLOCKED click on Off/Blur in SafeSearch! Enforcing FILTER!")
                    AntiPornDefenderManager.enforceSafeSearchFilter(browserRoot)
                    forceExitSafeSearchPage()
                    return
                }
            }

            // 2. Continuously lock SafeSearch ON FILTER if settings UI is open
            AntiPornDefenderManager.enforceSafeSearchFilter(browserRoot)

            // 3. FAST CHECK: Scan URL bar directly for SafeSearch URLs
            var fastSafeSearchDetected = false
            if (activeUrl != null && AntiPornDefenderManager.isNavigatingToSafeSearchSettings(activeUrl)) {
                fastSafeSearchDetected = true
            }

            val sourceViewId = try { event.source?.viewIdResourceName ?: "" } catch (_: Exception) { "" }
            val isUrlBarEvent = AntiPornDefenderManager.isKnownUrlBarId(sourceViewId) ||
                                sourceViewId.contains("url_bar") ||
                                sourceViewId.contains("omnibox") ||
                                sourceViewId.contains("search_box")

            if (!fastSafeSearchDetected && isUrlBarEvent) {
                val eventTextBuilder = StringBuilder()
                for (t in event.text) {
                    eventTextBuilder.append(t?.toString()?.lowercase() ?: "").append(' ')
                }
                val evSourceText = event.source?.text?.toString()?.lowercase() ?: ""
                eventTextBuilder.append(evSourceText).append(' ')
                val evDesc = event.contentDescription?.toString()?.lowercase() ?: ""
                eventTextBuilder.append(evDesc)
                val fastEventText = eventTextBuilder.toString()

                if (AntiPornDefenderManager.isNavigatingToSafeSearchSettings(fastEventText)) {
                    fastSafeSearchDetected = true
                }
            }

            // 4. FAST CHECK: Full window hierarchy scan for SafeSearch settings page
            if (!fastSafeSearchDetected) {
                val safeSearchPageDetection = AntiPornDefenderManager.detectSafeSearchSettingsPage(browserRoot)
                if (safeSearchPageDetection != null) {
                    fastSafeSearchDetected = true
                }
            }

            if (fastSafeSearchDetected) {
                // Ensure Filter mode is locked BEFORE exiting!
                AntiPornDefenderManager.enforceSafeSearchFilter(browserRoot)

                val now = System.currentTimeMillis()
                if (now - lastSafeSearchExitTimestamp > 300L) {
                    lastSafeSearchExitTimestamp = now
                    lastBlockedPackage = activeBrowser
                    lastBlockedTimestamp = now
                    Log.d(TAG, "🔒 NUCLEAR SafeSearch page block — forcing exit with FILTER locked")
                    forceExitSafeSearchPage()
                    return
                }
            }
        }

        if (AntiPornDefenderManager.isDefenderEnabled.value &&
            activeBrowser != null &&
            !isEssentialSafePackage(rawPackageName) &&
            rawPackageName != packageName
        ) {
            val browserRoot = getActiveBrowserRootNode(event)
            val activeUrl = AntiPornDefenderManager.extractActiveBrowserUrl(browserRoot)

            // CRITICAL: Localhost, internal test servers, browser history, and whitelisted clean sites (GitHub, etc.) are 100% exempt from adult blocker checks
            if ((activeUrl != null && (AntiPornDefenderManager.isLocalOrExemptUrl(activeUrl) || AntiPornDefenderManager.isCleanOrWhitelistedUrl(activeUrl))) ||
                AntiPornDefenderManager.isBrowserHistoryScreen(activeUrl, browserRoot)
            ) {
                return
            }

            val effectivePkg = activeBrowser
            var detectedTrigger: String? = null

            // 1. Immediate Event Text Check (catches text being typed, search queries, URL bar text)
            // CRITICAL: Only evaluate text if it originates from an editable input, search field, or URL bar!
            val sourceNode = try { event.source } catch (_: Exception) { null }
            val sourceId = sourceNode?.viewIdResourceName ?: ""
            val isEditableOrInput = sourceNode?.isEditable == true ||
                                    sourceNode?.className?.toString()?.contains("EditText", ignoreCase = true) == true ||
                                    AntiPornDefenderManager.isKnownUrlBarId(sourceId) ||
                                    event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                                    event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED

            if (isEditableOrInput) {
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
                    val sourceText = sourceNode?.text?.toString() ?: ""
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
            }

            // 4. SafeSearch & DNS Tamper Prevention: If user tries to disable SafeSearch or change DNS
            if (detectedTrigger == null && AntiPornDefenderManager.isSafeSearchEnforced.value && browserRoot != null) {
                detectedTrigger = AntiPornDefenderManager.detectSafeSearchTamper(event, browserRoot)
            }

            // 5. Deep Window Hierarchy Check on confirmed browser window
            if (detectedTrigger == null && browserRoot != null) {
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


                    val isSafeSearchOrDnsTamper = detectedTrigger.contains("SafeSearch", ignoreCase = true) ||
                                                  detectedTrigger.contains("Safe Search", ignoreCase = true) ||
                                                  detectedTrigger.contains("DNS", ignoreCase = true) ||
                                                  detectedTrigger.contains("safe=off", ignoreCase = true) ||
                                                  detectedTrigger.contains("safesearch=off", ignoreCase = true) ||
                                                  detectedTrigger.contains("safesearch=0", ignoreCase = true) ||
                                                  detectedTrigger.contains("adlt=off", ignoreCase = true) ||
                                                  detectedTrigger.contains("kp=-2", ignoreCase = true) ||
                                                  detectedTrigger.contains("preferences", ignoreCase = true) ||
                                                  detectedTrigger.contains("Navigation", ignoreCase = true) ||
                                                  detectedTrigger.contains("settings page", ignoreCase = true)

                    if (isSafeSearchOrDnsTamper) {
                        // NUCLEAR: Multi-step exit to guarantee the user cannot stay on the SafeSearch page!
                        forceExitSafeSearchPage()
                        return
                    }

                    // Strict practical block: double BACK, clear text, HOME fallback, toast
                    // Absolutely NO preachy "You Are Searching the Wrong Thing" popup screen!
                    hardBlockAdultContent(effectivePkg)
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
                flushForegroundTime(now)
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
                    flushForegroundTime(now)
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
                flushForegroundTime(now)
                endReelSessionIfActive()
                currentForegroundPackage = rawPackageName
                foregroundAppStartTime = 0L


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

                    // CRITICAL FIX: NEVER block user on Accessibility, Permission, or Restricted Settings screens!
                    val isSystemConfigurationScreen = combinedText.contains("accessibility") ||
                        combinedText.contains("installed apps") ||
                        combinedText.contains("downloaded apps") ||
                        combinedText.contains("downloaded services") ||
                        combinedText.contains("installed services") ||
                        combinedText.contains("accessibility service") ||
                        combinedText.contains("allow restricted") ||
                        combinedText.contains("restricted settings") ||
                        combinedText.contains("special app access") ||
                        combinedText.contains("usage access") ||
                        combinedText.contains("notification access") ||
                        combinedText.contains("battery optimization") ||
                        combinedText.contains("permission")

                    if (isSystemConfigurationScreen) {
                        return
                    }

                    if (isDnsTamperAttempt(combinedText)) {
                        Log.d(TAG, "🔒 Intercepted Private DNS settings attempt in Android Settings")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        android.widget.Toast.makeText(applicationContext, "🔒 FocusCrew: Private DNS settings are protected by Focus Shield", android.widget.Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Anti-Tamper: Protect FocusCrew ONLY from explicit Force-Stop or Uninstall during an active timer session
                    val isTimerActive = com.focuscrew.app.ui.timer.TimerManager.snapshot.value.remainingMs > 0
                    if (AppSecurityManager.isSettingsAntiTamperActive.value && isTimerActive) {
                        val isFocusCrewTarget = combinedText.contains("focuscrew") || combinedText.contains("focus crew")
                        val isDestructiveAction = combinedText.contains("force stop") || combinedText.contains("uninstall") || combinedText.contains("disable")

                        if (isFocusCrewTarget && isDestructiveAction) {
                            Log.d(TAG, "🔒 Intercepted FocusCrew Force-Stop/Uninstall attempt in Android Settings during active focus")
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
                enforceAppBlock(rawPackageName, isFreedomVaultCurfew, isTimerBlocked)
                return
            }
        }
    }

    /**
     * Strict Practical Adult Content Blocker.
     * Instantly exits explicit searches, clears search bar, and forces back/home.
     * Never shows any preachy popup screens!
     */
    private fun hardBlockAdultContent(browserPackage: String) {
        // Step 1: Immediate BACK press to cancel/exit the explicit search query or page navigation
        performGlobalAction(GLOBAL_ACTION_BACK)

        // Step 2: Try to clear or replace text in the browser search bar node
        try {
            val browserRoot = getActiveBrowserRootNode()
            if (browserRoot != null) {
                val urlBarNodes = browserRoot.findAccessibilityNodeInfosByViewId("$browserPackage:id/url_bar")
                    ?: browserRoot.findAccessibilityNodeInfosByViewId("$browserPackage:id/search_box_text")
                    ?: browserRoot.findAccessibilityNodeInfosByViewId("$browserPackage:id/googleapp_search_box")
                    ?: browserRoot.findAccessibilityNodeInfosByViewId("$browserPackage:id/open_search_view_edit_text")
                    ?: browserRoot.findAccessibilityNodeInfosByViewId("$browserPackage:id/search_src_text")
                    ?: browserRoot.findAccessibilityNodeInfosByViewId("$browserPackage:id/search_box")
                urlBarNodes?.firstOrNull()?.let { node ->
                    val arguments = android.os.Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                    }
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                }
            }
        } catch (_: Exception) {}

        // Step 3: Second BACK press after 180ms to ensure the tab/page is exited
        mainHandler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
        }, 180L)

        // Step 4: Strict Nuclear Fallback — If still showing explicit content in the browser after 450ms, go HOME
        mainHandler.postDelayed({
            val root = try { rootInActiveWindow } catch (_: Exception) { null }
            val activePkg = root?.packageName?.toString() ?: ""
            if (AntiPornDefenderManager.isSearchOrBrowserApp(activePkg)) {
                val stillDetected = AntiPornDefenderManager.scanBrowserForAdultContent(root)
                if (stillDetected != null) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
        }, 450L)

        // Step 5: Clean toast notification
        try {
            Toast.makeText(
                applicationContext,
                "🔒 FocusCrew Shield: Explicit search blocked & SafeSearch locked",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {}
    }

    /**
     * Reliable App Blocker Enforcement for Android 14/15.
     * Directly launches CalmBlockActivity via PendingIntent with foreground privilege,
     * with fallback to direct startActivity and HOME.
     */
    private fun enforceAppBlock(pkg: String, isCurfew: Boolean, isTimerBlocked: Boolean) {
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPackage && (now - lastBlockedTimestamp) < 600L) {
            return
        }
        lastBlockedPackage = pkg
        lastBlockedTimestamp = now

        val appLabel = getAppLabel(pkg)
        val displayLabel = if (isCurfew && !isTimerBlocked) {
            "$appLabel (Danger Zone Curfew)"
        } else {
            appLabel
        }
        Log.d(TAG, "Intercepting blocked app: $pkg ($displayLabel)")

        val blockType = if (isCurfew && !isTimerBlocked) {
            CalmBlockActivity.BLOCK_TYPE_CURFEW
        } else {
            CalmBlockActivity.BLOCK_TYPE_TIMER_APP
        }

        val blockIntent = Intent(this, CalmBlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(CalmBlockActivity.EXTRA_BLOCKED_PACKAGE, pkg)
            putExtra(CalmBlockActivity.EXTRA_BLOCKED_LABEL, displayLabel)
            putExtra(CalmBlockActivity.EXTRA_BLOCK_TYPE, blockType)
        }

        val optionsBundle = if (Build.VERSION.SDK_INT >= 34) {
            try {
                val method = ActivityOptions::class.java.getMethod("makeBasic")
                val basic = method.invoke(null) as ActivityOptions
                val setPendingMode = ActivityOptions::class.java.getMethod(
                    "setPendingIntentBackgroundActivityStartMode",
                    Int::class.javaPrimitiveType
                )
                setPendingMode.invoke(basic, 1) // MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                basic.toBundle()
            } catch (_: Exception) { null }
        } else null

        var launched = false
        try {
            val pending = PendingIntent.getActivity(
                this,
                pkg.hashCode(),
                blockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                optionsBundle
            )
            pending.send()
            launched = true
        } catch (_: Exception) {}

        if (!launched) {
            try {
                startActivity(blockIntent, optionsBundle)
                launched = true
            } catch (_: Exception) {
                try { startActivity(blockIntent); launched = true } catch (_: Exception) {}
            }
        }

        if (!launched) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    /**
     * NUCLEAR SafeSearch Page Exit — Guaranteed to remove the user from the SafeSearch settings page.
     * Step 1: Press BACK immediately (exits the page in most cases)
     * Step 2: Press BACK again after 200ms (catches single-page app scenarios where first BACK was absorbed)
     * Step 3: Press HOME after 500ms if still in browser (absolute nuclear fallback — takes user to home screen)
     * Step 4: Show Toast so user knows SafeSearch is locked
     */
    private fun forceExitSafeSearchPage() {
        // Step 1: Immediate BACK press
        performGlobalAction(GLOBAL_ACTION_BACK)

        // Step 2: Second BACK press after 200ms
        mainHandler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
        }, 200L)

        // Step 3: NUCLEAR FALLBACK — If still in browser after 500ms, press HOME
        mainHandler.postDelayed({
            val root = try { rootInActiveWindow } catch (_: Exception) { null }
            val activePkg = root?.packageName?.toString() ?: ""
            // If still inside a browser or search app, the page might still be showing — go HOME!
            if (AntiPornDefenderManager.isSearchOrBrowserApp(activePkg)) {
                val browserRoot = getActiveBrowserRootNode()
                val stillOnSafeSearchPage = AntiPornDefenderManager.detectSafeSearchSettingsPage(browserRoot)
                if (stillOnSafeSearchPage != null) {
                    Log.d(TAG, "🏠 NUCLEAR HOME — user still on SafeSearch page after double BACK, forcing HOME")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
        }, 500L)

        // Step 4: Toast notification
        try {
            android.widget.Toast.makeText(
                applicationContext,
                "🔒 SafeSearch is permanently locked by FocusCrew",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {}
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

    private fun resolveActiveBrowserPackage(eventPackage: String): String? {
        // 1. If event is explicitly from a non-browser app, strictly reject
        if (AntiPornDefenderManager.isNonBrowserApp(eventPackage)) {
            return null
        }

        // 2. Check the active window currently on screen
        val activeRoot = try { rootInActiveWindow } catch (_: Exception) { null }
        val activeWindowPkg = activeRoot?.packageName?.toString() ?: ""

        // If the active window on screen is a known non-browser app, strictly reject
        if (AntiPornDefenderManager.isNonBrowserApp(activeWindowPkg)) {
            return null
        }

        // 3. Direct browser or search app event when screen is in browser or neutral
        if (AntiPornDefenderManager.isSearchOrBrowserApp(eventPackage)) {
            if (activeWindowPkg.isEmpty() || AntiPornDefenderManager.isSearchOrBrowserApp(activeWindowPkg)) {
                return eventPackage
            }
        }

        // 4. Keyboard event: ONLY allowed if the active window on screen is a confirmed browser or search app!
        val isKeyboard = eventPackage == "com.google.android.inputmethod.latin" || eventPackage.contains("inputmethod")
        if (isKeyboard && AntiPornDefenderManager.isSearchOrBrowserApp(activeWindowPkg)) {
            return activeWindowPkg
        }

        // 5. Active window itself is confirmed to be a browser or search app
        if (AntiPornDefenderManager.isSearchOrBrowserApp(activeWindowPkg)) {
            return activeWindowPkg
        }

        return null
    }

    private fun getActiveBrowserRootNode(event: AccessibilityEvent? = null): AccessibilityNodeInfo? {
        val root = try { rootInActiveWindow } catch (_: Exception) { null }
        if (root != null && AntiPornDefenderManager.isSearchOrBrowserApp(root.packageName?.toString() ?: "")) {
            return root
        }
        val source = try { event?.source } catch (_: Exception) { null }
        if (source != null) {
            var current: AccessibilityNodeInfo? = source
            while (current?.parent != null) {
                current = current.parent
            }
            if (current != null && AntiPornDefenderManager.isSearchOrBrowserApp(current.packageName?.toString() ?: "")) {
                return current
            }
        }
        return root
    }

    companion object {
        private const val TAG = "FocusCrewBlocker"
    }
}
