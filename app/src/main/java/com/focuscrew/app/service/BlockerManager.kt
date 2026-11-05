package com.focuscrew.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory manager for the Blocker spike.
 * Coordinates between the UI and the BlockerAccessibilityService.
 */
object BlockerManager {
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _isBlockingActive = MutableStateFlow(false)
    val isBlockingActive: StateFlow<Boolean> = _isBlockingActive.asStateFlow()

    // Test blocked packages for this spike (YouTube and Chrome)
    private val _blockedPackages = MutableStateFlow(
        setOf(
            "com.google.android.youtube",
            "com.android.chrome",
            "com.instagram.android"
        )
    )
    val blockedPackages: StateFlow<Set<String>> = _blockedPackages.asStateFlow()

    private val _isWhitelistMode = MutableStateFlow(false)
    val isWhitelistMode: StateFlow<Boolean> = _isWhitelistMode.asStateFlow()

    private val _whitelistPackages = MutableStateFlow(
        setOf(
            "com.focuscrew.app",
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.google.android.keep",
            "com.google.android.apps.docs"
        )
    )
    val whitelistPackages: StateFlow<Set<String>> = _whitelistPackages.asStateFlow()

    private val _isAlwaysOnBlockingEnabled = MutableStateFlow(true)
    val isAlwaysOnBlockingEnabled: StateFlow<Boolean> = _isAlwaysOnBlockingEnabled.asStateFlow()

    private var sharedPrefs: android.content.SharedPreferences? = null
    private const val PREFS_NAME = "focuscrew_blocker_prefs"
    private const val KEY_ALWAYS_ON_BLOCKING = "always_on_blocking_enabled"

    fun initialize(context: android.content.Context) {
        if (sharedPrefs == null) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            sharedPrefs = prefs
            _isAlwaysOnBlockingEnabled.value = prefs.getBoolean(KEY_ALWAYS_ON_BLOCKING, true)
        }
    }

    fun setAlwaysOnBlocking(enabled: Boolean) {
        _isAlwaysOnBlockingEnabled.value = enabled
        sharedPrefs?.edit()?.putBoolean(KEY_ALWAYS_ON_BLOCKING, enabled)?.apply()
    }

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    fun setBlockingActive(active: Boolean) {
        _isBlockingActive.value = active
    }

    fun setWhitelistMode(enabled: Boolean) {
        _isWhitelistMode.value = enabled
    }

    fun setWhitelistPackages(packages: Set<String>) {
        _whitelistPackages.value = packages
    }

    fun toggleWhitelistPackage(packageName: String) {
        val current = _whitelistPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            if (packageName != "com.focuscrew.app") {
                current.remove(packageName)
            }
        } else {
            current.add(packageName)
        }
        _whitelistPackages.value = current
    }

    fun setBlockedPackages(packages: Set<String>) {
        _blockedPackages.value = packages
    }

    fun togglePackage(packageName: String) {
        val current = _blockedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _blockedPackages.value = current
    }

    fun isPackageBlocked(packageName: String): Boolean {
        // Block if active timer session blocking is ON OR Always-On blocking is enabled!
        if (!_isBlockingActive.value && !_isAlwaysOnBlockingEnabled.value) return false
        
        if (_isWhitelistMode.value) {
            // In Whitelist mode, block if NOT in whitelist
            return !_whitelistPackages.value.contains(packageName)
        }
        
        return _blockedPackages.value.contains(packageName)
    }
}
