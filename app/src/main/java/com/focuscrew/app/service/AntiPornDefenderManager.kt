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
        "com.google.android.googlequicksearchbox", // Google Search Bar / App
        "com.google.android.apps.searchlite",   // Google Go
        "com.android.browser",                  // AOSP Browser
        "com.ucmobile.intl"                     // UC Browser
    )

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
        "com.microsoft.emmx:id/url_bar",
        "com.google.android.googlequicksearchbox:id/googleapp_search_box",
        "com.google.android.googlequicksearchbox:id/search_edit_frame"
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
        _isSafeSearchEnforced.value = p.getBoolean(KEY_SAFESEARCH_ENABLED, true)
    }

    fun setDefenderEnabled(enabled: Boolean) {
        _isDefenderEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_DEFENDER_ENABLED, enabled)?.apply()
    }

    fun setSafeSearchEnforced(enforced: Boolean) {
        _isSafeSearchEnforced.value = enforced
        prefs?.edit()?.putBoolean(KEY_SAFESEARCH_ENABLED, enforced)?.apply()
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
        if (SUPPORTED_BROWSER_PACKAGES.contains(packageName)) return true
        val lower = packageName.lowercase()
        return lower.contains("browser") ||
               lower.contains("chrome") ||
               lower.contains("firefox") ||
               lower.contains("opera") ||
               lower.contains("searchbox") ||
               lower.contains("search") ||
               lower.contains("duckduckgo") ||
               lower.contains("webview") ||
               lower.contains("sbrowser") ||
               lower.contains("emmx")
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

    private fun scanNodeForSafeSearchTamper(node: AccessibilityNodeInfo?, depth: Int, maxDepth: Int): String? {
        if (node == null || depth > maxDepth) return null

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val combined = "$text $desc"

        if (combined.contains("google.com/safesearch") ||
            combined.contains("safe=off") ||
            combined.contains("safe=images") ||
            combined.contains("safesearch=off") ||
            combined.contains("safesearch=0") ||
            combined.contains("adlt=off")
        ) {
            return "SafeSearch Locked: Titan Fortress permanently locks SafeSearch on!"
        }

        // Only trigger on explicit intent to turn off SafeSearch, NEVER on Google's own "SafeSearch: Filter" or "Manage SafeSearch" labels!
        if (combined.contains("turn off safesearch") ||
            combined.contains("turn safesearch off") ||
            combined.contains("disable safesearch") ||
            combined.contains("disable safe search") ||
            combined.contains("turn off safe search")
        ) {
            return "SafeSearch Locked: Titan Fortress permanently locks SafeSearch on!"
        }

        // DNS Tamper Detection in Browser (Secure DNS / Custom DNS / Provider switches)
        if (combined.contains("use secure dns") ||
            combined.contains("choose another provider") ||
            combined.contains("custom dns")
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

        // Only inspect text if this node is an input field, address/URL bar, search bar, or page title!
        // This ensures news articles, blogs, research, and Wikipedia pages are NEVER falsely blocked!
        if (isInputOrUrl) {
            val text = node.text?.toString() ?: ""
            if (text.isNotBlank()) {
                val trigger = detectAdultContent(text)
                if (trigger != null) return trigger
            }

            val desc = node.contentDescription?.toString() ?: ""
            if (desc.isNotBlank()) {
                val descTrigger = detectAdultContent(desc)
                if (descTrigger != null) return descTrigger
            }

            val hint = node.hintText?.toString() ?: ""
            if (hint.isNotBlank()) {
                val hintTrigger = detectAdultContent(hint)
                if (hintTrigger != null) return hintTrigger
            }
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
