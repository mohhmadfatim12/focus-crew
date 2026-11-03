package com.focuscrew.app.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import com.focuscrew.app.domain.AdultDomainBlocklist
import com.focuscrew.app.domain.FuzzyAdultContentDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Anti-Porn Defender Manager.
 * 100% Offline, independent module.
 *
 * Module 1: Browser Search-Bar & URL Keyword Interceptor (Chrome, Brave, Opera, Firefox, etc.)
 *           NOW POWERED BY FuzzyAdultContentDetector (4-layer: exact + leetspeak + phonetic + ngram)
 *           and AdultDomainBlocklist (1000+ domain blackhole)
 * Module 2: SafeSearch Enforcer & Adult Content Filter
 * Module 3: 1-Tap "Purge Browser Traces" (Clean Slate)
 */
object AntiPornDefenderManager {

    private const val PREFS_NAME = "focuscrew_anti_porn_defender"
    private const val KEY_DEFENDER_ENABLED = "defender_enabled"
    private const val KEY_BLOCKED_SEARCHES_COUNT = "blocked_searches_count"
    private const val KEY_BLOCKED_SITES_COUNT = "blocked_sites_count"
    private const val KEY_SAFESEARCH_ENABLED = "safesearch_enabled"

    private var prefs: SharedPreferences? = null

    private val _isDefenderEnabled = MutableStateFlow(true)
    val isDefenderEnabled: StateFlow<Boolean> = _isDefenderEnabled.asStateFlow()

    private val _blockedSearchesCount = MutableStateFlow(0)
    val blockedSearchesCount: StateFlow<Int> = _blockedSearchesCount.asStateFlow()

    private val _blockedSitesCount = MutableStateFlow(0)
    val blockedSitesCount: StateFlow<Int> = _blockedSitesCount.asStateFlow()

    private val _isSafeSearchEnforced = MutableStateFlow(true)
    val isSafeSearchEnforced: StateFlow<Boolean> = _isSafeSearchEnforced.asStateFlow()

    val SUPPORTED_BROWSER_PACKAGES = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.gx",
        "com.opera.touch",
        "org.mozilla.firefox",
        "org.mozilla.focus",
        "org.torproject.torbrowser",
        "com.sec.android.app.sbrowser",         // Samsung Internet
        "com.microsoft.emmx",                   // Microsoft Edge
        "com.duckduckgo.mobile.android",        // DuckDuckGo
        "com.vivaldi.browser",                  // Vivaldi
        "com.kiwibrowser.browser",              // Kiwi Browser
        "com.android.browser",                  // AOSP Browser
        "com.ucmobile.intl"                     // UC Browser
    )

    val NON_BROWSER_PACKAGES = setOf(
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
        "com.skype.raider",
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.netflix.mediaclient",
        "com.spotify.music",
        "com.reddit.frontpage",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.tinder",
        "com.bumble.app",
        "com.motorola.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3",
        "com.mi.android.globallauncher",
        "com.sec.android.app.launcher",
        "com.oppo.launcher",
        "com.vivo.launcher",
        "com.transsion.launcher",
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "com.tencent.ig",
        "com.pubg.imobile",
        "com.pubg.krmobile"
    )

    fun isNonBrowserApp(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (NON_BROWSER_PACKAGES.contains(packageName)) return true
        val lower = packageName.lowercase()
        return lower.contains("whatsapp") ||
               lower.contains("telegram") ||
               lower.contains("instagram") ||
               lower.contains("facebook") ||
               lower.contains("messenger") ||
               lower.contains("snapchat") ||
               lower.contains("discord") ||
               lower.contains("twitter") ||
               lower.contains("youtube") ||
               lower.contains("tiktok") ||
               lower.contains("reddit") ||
               lower.contains("tinder") ||
               lower.contains("bumble") ||
               lower.contains("netflix") ||
               lower.contains("spotify") ||
               lower.contains("game") ||
               lower.contains("launcher") ||
               lower.contains("quicksearchbox")
    }

    private val KNOWN_URL_BAR_IDS = setOf(
        "com.android.chrome:id/url_bar",
        "com.android.chrome:id/search_box_text",
        "com.android.chrome:id/omnibox_text",
        "com.android.chrome:id/location_bar",
        "com.android.chrome:id/toolbar",
        "com.brave.browser:id/url_bar",
        "com.brave.browser:id/search_box_text",
        "com.brave.browser:id/omnibox_text",
        "com.opera.browser:id/url_field",
        "org.mozilla.firefox:id/toolbar_url_bar",
        "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "com.microsoft.emmx:id/search_box",
        "com.microsoft.emmx:id/url_bar"
    )


    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadPreferences()
        }
    }

    private fun loadPreferences() {
        val p = prefs ?: return
        _isDefenderEnabled.value = p.getBoolean(KEY_DEFENDER_ENABLED, true)
        _blockedSearchesCount.value = p.getInt(KEY_BLOCKED_SEARCHES_COUNT, 0)
        _blockedSitesCount.value = p.getInt(KEY_BLOCKED_SITES_COUNT, 0)
        // SafeSearch is PERMANENTLY locked on — user can never disable it!
        _isSafeSearchEnforced.value = true
        p.edit().putBoolean(KEY_SAFESEARCH_ENABLED, true).apply()
    }

    fun setDefenderEnabled(enabled: Boolean) {
        _isDefenderEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_DEFENDER_ENABLED, enabled)?.apply()
    }

    fun setSafeSearchEnforced(enforced: Boolean = true) {
        // SafeSearch is permanently locked on — user can never turn it off!
        _isSafeSearchEnforced.value = true
        prefs?.edit()?.putBoolean(KEY_SAFESEARCH_ENABLED, true)?.apply()
    }

    fun recordSearchBlocked() {
        val next = _blockedSearchesCount.value + 1
        _blockedSearchesCount.value = next
        prefs?.edit()?.putInt(KEY_BLOCKED_SEARCHES_COUNT, next)?.apply()
    }

    fun recordSiteBlocked() {
        val next = _blockedSitesCount.value + 1
        _blockedSitesCount.value = next
        prefs?.edit()?.putInt(KEY_BLOCKED_SITES_COUNT, next)?.apply()
    }

    fun isBrowserPackage(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (isNonBrowserApp(packageName)) return false
        if (SUPPORTED_BROWSER_PACKAGES.contains(packageName)) return true
        val lower = packageName.lowercase()
        // Never treat launchers, file browsers, media viewers, system components, or webviews as browsers!
        if (lower.contains("file") || lower.contains("media") || lower.contains("photo") ||
            lower.contains("gallery") || lower.contains("launcher") ||
            lower.contains("webview") || lower.contains("setting") || lower.contains("system") ||
            lower.contains("camera")) {
            return false
        }
        return (lower.endsWith(".browser") || lower.contains(".browser.") || lower.contains("browser_")) ||
               lower.contains("chrome") ||
               lower.contains("firefox") ||
               lower.contains("opera") ||
               lower.contains("duckduckgo") ||
               lower.contains("sbrowser") ||
               lower.contains("emmx")
    }

    fun isSearchOrBrowserApp(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (isBrowserPackage(packageName)) return true
        val lower = packageName.lowercase()
        return lower == "com.google.android.googlequicksearchbox" ||
               lower == "com.google.android.apps.searchlite" ||
               lower.contains("searchbox")
    }

    /**
     * Checks if a string contains adult keywords or porn domains.
     * NOW uses FuzzyAdultContentDetector 4-layer pipeline + AdultDomainBlocklist:
     *   Layer 1: Exact keyword (500+) & domain matching
     *   Layer 2: Leetspeak & homoglyph decode → re-check ("p0rn" → "porn")
     *   Layer 3: Phonetic fingerprint matching ("comatoze xxx" caught)
     *   Layer 4: N-gram fuzzy substring detection (creative misspellings)
     *   Layer 5: AdultDomainBlocklist 1000+ domain check
     *
     * Returns the matched trigger phrase, or null if clean.
     */
    fun detectAdultContent(queryOrUrl: String): String? {
        val trigger = FuzzyAdultContentDetector.detect(queryOrUrl)
        if (trigger != null) return trigger

        if (AdultDomainBlocklist.isUrlBlocked(queryOrUrl) || AdultDomainBlocklist.isBlocked(queryOrUrl)) {
            return "Adult Domain: $queryOrUrl"
        }
        return null
    }

    /**
     * Inspects browser view hierarchy to detect if the user typed an adult search,
     * opened an adult website, viewed explicit image thumbnails, or attempted to disable SafeSearch.
     */
    fun scanBrowserForAdultContent(rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null) return null

        // 1. Fast check against known URL / Search bar IDs
        for (viewId in KNOWN_URL_BAR_IDS) {
            try {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                if (!nodes.isNullOrEmpty()) {
                    for (node in nodes) {
                        val text = node.text?.toString() ?: ""
                        val trigger = detectAdultContent(text)
                        if (trigger != null) return trigger

                        val contentDesc = node.contentDescription?.toString() ?: ""
                        val descTrigger = detectAdultContent(contentDesc)
                        if (descTrigger != null) return descTrigger
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. SafeSearch Tamper Detection across whole active window
        if (_isSafeSearchEnforced.value) {
            val tamper = scanNodeForSafeSearchTamper(rootNode, depth = 0, maxDepth = 30)
            if (tamper != null) return tamper
        }

        // 3. Deep Breadth/Depth search for adult content, URL bars, titles, and image previews
        try {
            val trigger = searchNodeRecursive(rootNode, depth = 0, maxDepth = 35)
            if (trigger != null) return trigger
        } catch (_: Exception) {}

        return null
    }

    /**
     * SafeSearch Settings Page URLs that must be IMMEDIATELY blocked.
     * If the user navigates to ANY of these, press BACK instantly — don't even let them see the page!
     */
    private val SAFESEARCH_SETTINGS_URLS = listOf(
        "google.com/safesearch",
        "google.com/preferences",
        "google.com/setprefs",
        "google.com.pk/safesearch",
        "google.co.in/safesearch",
        "google.co.uk/safesearch",
        "bing.com/account/general",
        "bing.com/account",
        "bing.com/preferences",
        "duckduckgo.com/settings",
        "duckduckgo.com/?kp=",
        "search.yahoo.com/preferences",
        "yandex.com/search/customize"
    )

    /**
     * Checks if the URL bar text is pointing to a SafeSearch settings page.
     * Returns true if the URL should be IMMEDIATELY blocked.
     */
    fun isNavigatingToSafeSearchSettings(urlText: String): Boolean {
        val lower = urlText.lowercase().trim()
        for (pattern in SAFESEARCH_SETTINGS_URLS) {
            if (lower.contains(pattern)) return true
        }
        // Catch URL parameters that disable SafeSearch
        if (lower.contains("safe=off") ||
            lower.contains("safesearch=off") ||
            lower.contains("safesearch=0") ||
            lower.contains("adlt=off") ||
            lower.contains("adlt=demote") ||
            lower.contains("kp=-2") ||
            lower.contains("safe=images")
        ) return true
        return false
    }

    /**
     * PROACTIVE SafeSearch settings page detection.
     * Scans the browser window hierarchy for SafeSearch settings page content.
     * If detected, returns a trigger string IMMEDIATELY so Layer 2 presses BACK
     * before the user can even see the SafeSearch options.
     */
    fun detectSafeSearchSettingsPage(rootNode: AccessibilityNodeInfo?): String? {
        if (rootNode == null || !_isSafeSearchEnforced.value) return null

        // 1. Check URL bar — is user navigating to a SafeSearch settings page?
        for (viewId in KNOWN_URL_BAR_IDS) {
            try {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                if (!nodes.isNullOrEmpty()) {
                    for (node in nodes) {
                        val url = node.text?.toString() ?: ""
                        if (url.isNotBlank() && isNavigatingToSafeSearchSettings(url)) {
                            return "SafeSearch Locked: Navigation to SafeSearch settings is blocked!"
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Scan full window text for SafeSearch settings page indicators
        val allTexts = mutableListOf<String>()
        collectVisibleTexts(rootNode, allTexts, depth = 0, maxDepth = 40)
        val windowCombined = allTexts.joinToString(" ").lowercase()

        // Google SafeSearch settings page detection
        val isGoogleSafeSearchPage = windowCombined.contains("google.com/safesearch") ||
            (windowCombined.contains("explicit results filter") && windowCombined.contains("filter") && windowCombined.contains("blur")) ||
            (windowCombined.contains("safesearch") && windowCombined.contains("filter") && windowCombined.contains("blur") && windowCombined.contains("off"))

        if (isGoogleSafeSearchPage) {
            // Auto-click Filter before pressing BACK to ensure SafeSearch stays locked!
            tryEnforceFilterMode(rootNode)
            return "SafeSearch Locked: Google SafeSearch settings page is blocked!"
        }

        // Bing SafeSearch settings page detection
        val isBingSafeSearchPage = (windowCombined.contains("bing.com/account") || windowCombined.contains("bing.com/preferences")) &&
            (windowCombined.contains("safesearch") || windowCombined.contains("safe search"))

        if (isBingSafeSearchPage) {
            return "SafeSearch Locked: Bing SafeSearch settings page is blocked!"
        }

        // DuckDuckGo SafeSearch settings page detection
        val isDdgSafeSearchPage = windowCombined.contains("duckduckgo.com/settings") ||
            (windowCombined.contains("duckduckgo") && windowCombined.contains("safe search"))

        if (isDdgSafeSearchPage) {
            return "SafeSearch Locked: DuckDuckGo SafeSearch settings page is blocked!"
        }

        // Yahoo SafeSearch settings page detection
        val isYahooSafeSearchPage = windowCombined.contains("search.yahoo.com/preferences") ||
            (windowCombined.contains("yahoo") && windowCombined.contains("safesearch"))

        if (isYahooSafeSearchPage) {
            return "SafeSearch Locked: Yahoo SafeSearch settings page is blocked!"
        }

        return null
    }

    fun detectSafeSearchTamper(event: android.view.accessibility.AccessibilityEvent?, rootNode: AccessibilityNodeInfo?): String? {
        if (!_isSafeSearchEnforced.value) return null

        val eventTexts = mutableListOf<String>()
        if (event != null) {
            for (t in event.text) {
                val str = t?.toString()?.lowercase() ?: ""
                if (str.isNotBlank()) eventTexts.add(str)
            }
            val sourceText = event.source?.text?.toString()?.lowercase() ?: ""
            if (sourceText.isNotBlank()) eventTexts.add(sourceText)
            val contentDesc = event.contentDescription?.toString()?.lowercase() ?: ""
            if (contentDesc.isNotBlank()) eventTexts.add(contentDesc)
        }
        val eventCombined = eventTexts.joinToString(" ")

        // 0. PROACTIVE: Detect if URL bar is navigating to SafeSearch settings
        if (isNavigatingToSafeSearchSettings(eventCombined)) {
            return "SafeSearch Locked: Navigation to SafeSearch settings is blocked!"
        }

        // 1. Direct SafeSearch bypass parameter in event text or omnibox
        if (eventCombined.contains("safesearch=off") ||
            eventCombined.contains("safesearch=0") ||
            eventCombined.contains("safe=off") ||
            eventCombined.contains("safe=images") ||
            eventCombined.contains("adlt=off") ||
            eventCombined.contains("adlt=demote") ||
            eventCombined.contains("kp=-2")
        ) {
            return "SafeSearch Locked: SafeSearch is permanently locked on!"
        }

        // 2. Direct intent to disable SafeSearch
        if (eventCombined.contains("turn off safesearch") ||
            eventCombined.contains("turn safesearch off") ||
            eventCombined.contains("disable safesearch") ||
            eventCombined.contains("disable safe search") ||
            eventCombined.contains("turn off safe search") ||
            eventCombined.contains("turn safe search off") ||
            eventCombined.contains("safesearch: off") ||
            eventCombined.contains("safesearch is off") ||
            eventCombined.contains("safesearch off") ||
            eventCombined.contains("safe search off") ||
            eventCombined.contains("turn off explicit results") ||
            eventCombined.contains("how to disable safesearch") ||
            eventCombined.contains("how to turn off safesearch") ||
            eventCombined.contains("remove safesearch") ||
            eventCombined.contains("unlock safesearch")
        ) {
            return "SafeSearch Locked: SafeSearch is permanently locked on!"
        }

        // 3. PROACTIVE PAGE DETECTION: If the full window is showing a SafeSearch settings page, block it immediately!
        if (rootNode != null) {
            val pageDetection = detectSafeSearchSettingsPage(rootNode)
            if (pageDetection != null) return pageDetection
        }

        // 4. Whole-window hierarchy analysis for remaining cases
        if (rootNode != null) {
            val allTexts = mutableListOf<String>()
            collectVisibleTexts(rootNode, allTexts, depth = 0, maxDepth = 40)
            val windowCombined = allTexts.joinToString(" ").lowercase()

            val isSafeSearchSettings = windowCombined.contains("google.com/safesearch") ||
                (windowCombined.contains("safesearch") && (windowCombined.contains("filter") || windowCombined.contains("blur") || windowCombined.contains("off")))

            if (isSafeSearchSettings) {
                // If user is inside SafeSearch settings:
                // ANY interaction is blocked — don't wait for them to tap Off or Blur!
                // Auto-enforce Filter mode then kick them out!
                tryEnforceFilterMode(rootNode)
                return "SafeSearch Locked: SafeSearch Filter mode is permanently locked on!"
            }

            // Bing SafeSearch settings in window
            if ((windowCombined.contains("strict") || windowCombined.contains("moderate") || windowCombined.contains("off")) &&
                windowCombined.contains("safesearch") && windowCombined.contains("bing")) {
                return "SafeSearch Locked: Bing SafeSearch settings are permanently locked!"
            }

            // Secure DNS Tamper Prevention
            if (windowCombined.contains("use secure dns") ||
                windowCombined.contains("choose another provider") ||
                windowCombined.contains("custom dns") ||
                windowCombined.contains("private dns")
            ) {
                return "DNS Shield Lock: Secure DNS cannot be changed to bypass protection!"
            }
        }

        return null
    }

    /**
     * UNBREAKABLE SafeSearch Filter Lock Engine:
     * 1. Detects if the current screen is a SafeSearch settings interface (Google, Bing, Yahoo, DuckDuckGo).
     * 2. Automatically clicks "Filter" (or "Strict") if it is not already checked.
     * 3. Prevents "Off" or "Blur" from remaining selected.
     */
    fun enforceSafeSearchFilter(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null || !_isSafeSearchEnforced.value) return false

        // Check visible texts
        val allTexts = mutableListOf<String>()
        collectVisibleTexts(rootNode, allTexts, depth = 0, maxDepth = 40)
        val combined = allTexts.joinToString(" ").lowercase()

        val isSafeSearchContext = (combined.contains("safesearch") || combined.contains("safe search")) &&
            (combined.contains("filter") || combined.contains("blur") || combined.contains("off") || combined.contains("strict")) ||
            combined.contains("explicit results") ||
            combined.contains("google.com/safesearch") ||
            combined.contains("bing.com/account") ||
            combined.contains("duckduckgo.com/settings")

        if (!isSafeSearchContext) return false

        // 1. Locate the "Filter" / "Strict" node
        var filterNode: AccessibilityNodeInfo? = null
        var isFilterChecked = false

        fun searchFilter(node: AccessibilityNodeInfo?) {
            if (node == null || filterNode != null) return
            val t = node.text?.toString()?.trim() ?: ""
            val d = node.contentDescription?.toString()?.trim() ?: ""

            val isMatch = t.equals("Filter", ignoreCase = true) ||
                t.equals("Strict", ignoreCase = true) ||
                t.startsWith("Filter (", ignoreCase = true) ||
                t.startsWith("Strict (", ignoreCase = true) ||
                (t.contains("Filter", ignoreCase = true) && (t.contains("explicit", ignoreCase = true) || t.contains("hide", ignoreCase = true))) ||
                d.equals("Filter", ignoreCase = true) ||
                d.equals("Strict", ignoreCase = true)

            if (isMatch) {
                filterNode = node
                if (node.isChecked) {
                    isFilterChecked = true
                }
                return
            }

            for (i in 0 until node.childCount) {
                searchFilter(node.getChild(i))
                if (filterNode != null) break
            }
        }

        searchFilter(rootNode)

        // 2. Also check if "Off" or "Blur" is currently checked
        var isOffOrBlurChecked = false
        fun checkOffOrBlur(node: AccessibilityNodeInfo?) {
            if (node == null || isOffOrBlurChecked) return
            val t = node.text?.toString()?.trim() ?: ""
            val d = node.contentDescription?.toString()?.trim() ?: ""

            val isOffOrBlur = t.equals("Off", ignoreCase = true) ||
                t.equals("Blur", ignoreCase = true) ||
                t.equals("Moderate", ignoreCase = true) ||
                t.startsWith("Off (", ignoreCase = true) ||
                d.equals("Off", ignoreCase = true) ||
                d.equals("Blur", ignoreCase = true)

            if (isOffOrBlur && node.isChecked) {
                isOffOrBlurChecked = true
                return
            }

            for (i in 0 until node.childCount) {
                checkOffOrBlur(node.getChild(i))
                if (isOffOrBlurChecked) break
            }
        }
        checkOffOrBlur(rootNode)

        // If Filter is NOT checked, OR if Off/Blur IS checked -> FORCE CLICK "Filter"!
        if (filterNode != null && (!isFilterChecked || isOffOrBlurChecked)) {
            var clicked = false
            if (filterNode!!.isClickable || filterNode!!.isCheckable) {
                clicked = filterNode!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            if (!clicked) {
                // Try parent nodes
                var p = filterNode!!.parent
                var depth = 0
                while (p != null && depth < 4 && !clicked) {
                    if (p.isClickable || p.isCheckable) {
                        clicked = p.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        break
                    }
                    p = p.parent
                    depth++
                }
            }
            if (clicked) {
                android.util.Log.d("AntiPornDefender", "🔒 SafeSearch successfully FORCE-LOCKED ON FILTER!")
                return true
            }
        }

        return isFilterChecked
    }

    /**
     * Checks if the user is actively attempting to tap "Off" or "Blur" in SafeSearch.
     */
    fun isAttemptingToDisableSafeSearch(event: android.view.accessibility.AccessibilityEvent?, rootNode: AccessibilityNodeInfo?): Boolean {
        if (!_isSafeSearchEnforced.value) return false
        val source = try { event?.source } catch (_: Exception) { null }
        val clickedText = source?.text?.toString()?.trim() ?: ""
        val clickedDesc = source?.contentDescription?.toString()?.trim() ?: ""
        val eventTexts = event?.text?.joinToString(" ") { it?.toString() ?: "" } ?: ""
        val clickCombined = "$clickedText $clickedDesc $eventTexts".lowercase()

        val isOffOrBlurAttempt = clickCombined.contains("off") ||
            clickCombined.contains("blur") ||
            clickCombined.contains("turn off") ||
            clickCombined.contains("moderate") ||
            clickCombined.contains("don't filter")

        if (!isOffOrBlurAttempt) return false

        // Check if current screen is SafeSearch
        val allTexts = mutableListOf<String>()
        collectVisibleTexts(rootNode, allTexts, depth = 0, maxDepth = 35)
        val windowCombined = allTexts.joinToString(" ").lowercase()

        return (windowCombined.contains("safesearch") || windowCombined.contains("safe search")) &&
               (windowCombined.contains("filter") || windowCombined.contains("explicit") || windowCombined.contains("google.com/safesearch"))
    }

    private fun tryEnforceFilterMode(node: AccessibilityNodeInfo?) {
        enforceSafeSearchFilter(node)
    }

    private fun collectVisibleTexts(node: AccessibilityNodeInfo?, outList: MutableList<String>, depth: Int, maxDepth: Int) {
        if (node == null || depth > maxDepth) return
        val t = node.text?.toString()
        if (!t.isNullOrBlank()) outList.add(t)
        val d = node.contentDescription?.toString()
        if (!d.isNullOrBlank()) outList.add(d)

        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            collectVisibleTexts(child, outList, depth + 1, maxDepth)
        }
    }

    private fun scanNodeForSafeSearchTamper(node: AccessibilityNodeInfo?, depth: Int, maxDepth: Int): String? {
        if (node == null || depth > maxDepth) return null

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val combined = "$text $desc"

        // URL-bar level: Detect navigation to SafeSearch settings pages
        if (isNavigatingToSafeSearchSettings(combined)) {
            return "SafeSearch Locked: Navigation to SafeSearch settings is blocked!"
        }

        if (combined.contains("safe=off") ||
            combined.contains("safe=images") ||
            combined.contains("safesearch=off") ||
            combined.contains("safesearch=0") ||
            combined.contains("adlt=off") ||
            combined.contains("adlt=demote") ||
            combined.contains("kp=-2")
        ) {
            return "SafeSearch Locked: Titan Fortress permanently locks SafeSearch on!"
        }

        // Catch explicit requests to turn off or disable SafeSearch
        if (combined.contains("turn off safesearch") ||
            combined.contains("turn safesearch off") ||
            combined.contains("disable safesearch") ||
            combined.contains("disable safe search") ||
            combined.contains("turn off safe search") ||
            combined.contains("turn safe search off") ||
            combined.contains("safesearch off") ||
            combined.contains("safe search off") ||
            combined.contains("how to turn off safesearch") ||
            combined.contains("safesearch: off") ||
            combined.contains("safesearch is off") ||
            combined.contains("off - turn off explicit") ||
            combined.contains("turn off explicit results") ||
            combined.contains("how to disable safesearch") ||
            combined.contains("remove safesearch") ||
            combined.contains("unlock safesearch") ||
            combined.contains("bypass safesearch")
        ) {
            return "SafeSearch Locked: Titan Fortress permanently locks SafeSearch on!"
        }

        // SafeSearch settings page indicators (Google, Bing, DuckDuckGo, Yahoo)
        // Detect the page even if user hasn't clicked anything yet!
        if (combined.contains("google.com/safesearch") ||
            combined.contains("explicit results filter") ||
            (combined.contains("bing.com/account") && (combined.contains("safesearch") || combined.contains("safe search"))) ||
            (combined.contains("bing.com/preferences") && (combined.contains("safesearch") || combined.contains("safe search"))) ||
            combined.contains("duckduckgo.com/settings") ||
            combined.contains("search.yahoo.com/preferences")
        ) {
            return "SafeSearch Locked: SafeSearch settings page is blocked!"
        }

        // DNS Tamper Detection in Browser (Secure DNS / Custom DNS / Provider switches)
        if (combined.contains("use secure dns") ||
            combined.contains("choose another provider") ||
            combined.contains("custom dns") ||
            combined.contains("private dns") ||
            combined.contains("dns provider hostname")
        ) {
            return "DNS Shield Lock: Secure DNS cannot be changed to bypass protection!"
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            val result = scanNodeForSafeSearchTamper(child, depth + 1, maxDepth)
            if (result != null) return result
        }

        return null
    }

    private fun searchNodeRecursive(node: AccessibilityNodeInfo?, depth: Int, maxDepth: Int): String? {
        if (node == null || depth > maxDepth) return null

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val isInputOrUrl = node.isEditable ||
                      node.className?.toString()?.contains("EditText", ignoreCase = true) == true ||
                      viewId.contains("url") ||
                      viewId.contains("search") ||
                      viewId.contains("omnibox") ||
                      viewId.contains("address") ||
                      viewId.contains("query") ||
                      viewId.contains("title")

        val text = node.text?.toString() ?: ""
        if (text.isNotBlank()) {
            if (isInputOrUrl || text.length <= 300) {
                val trigger = detectAdultContent(text)
                if (trigger != null) return trigger
            }
        }

        val desc = node.contentDescription?.toString() ?: ""
        if (desc.isNotBlank()) {
            // Google Images thumbnail descriptions can be up to 500 characters long
            if (isInputOrUrl || desc.length <= 500) {
                val descTrigger = detectAdultContent(desc)
                if (descTrigger != null) return descTrigger
            }
        }

        val hint = node.hintText?.toString() ?: ""
        if (hint.isNotBlank()) {
            val hintTrigger = detectAdultContent(hint)
            if (hintTrigger != null) return hintTrigger
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            val result = searchNodeRecursive(child, depth + 1, maxDepth)
            if (result != null) return result
        }

        return null
    }


    /**
     * Module 3: 1-Tap "Purge Browser Traces" (Clean Slate)
     * Clears clipboard and builds an intent to take the user directly
     * to Chrome's native Clear Browsing Data dialog.
     */
    fun executePurgeTraces(context: Context): Intent {
        // 1. Wipe clipboard history if any adult text was copied
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        } catch (_: Exception) {}

        // 2. Build Direct Intent to Chrome Clear Browsing Data
        val chromeIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("chrome://settings/clearBrowserData")
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Verify if Chrome handles chrome://settings
        val pm = context.packageManager
        if (chromeIntent.resolveActivity(pm) != null) {
            return chromeIntent
        }

        // Fallback: Open Chrome application details settings
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", "com.android.chrome", null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
