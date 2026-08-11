package com.focuscrew.app.service

import android.content.Context
import android.content.SharedPreferences
import com.focuscrew.app.data.model.BlockProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Milestone 9: Block Profiles Manager.
 *
 * Manages preset profiles (Study Mode, Coding & Work, Strict Lockdown)
 * and custom user profiles, persisting changes offline with zero external dependencies.
 */
object BlockProfileManager {

    private const val PREFS_NAME = "focuscrew_block_profiles"
    private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    private const val PREFIX_PROFILE_PKGS = "profile_pkgs_"
    private const val KEY_CUSTOM_PROFILES_LIST = "custom_profiles_list"

    private val DEFAULT_STUDY_PKGS = setOf(
        "com.google.android.youtube",
        "com.android.chrome",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.facebook.katana",
        "com.reddit.frontpage",
        "com.snapchat.android"
    )

    private val DEFAULT_CODING_PKGS = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.netflix.mediaclient",
        "tv.twitch.android.app"
    )

    private val DEFAULT_LOCKDOWN_PKGS = setOf(
        "com.google.android.youtube",
        "com.android.chrome",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.facebook.katana",
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.netflix.mediaclient",
        "tv.twitch.android.app",
        "com.spotify.music"
    )

    private val _profiles = MutableStateFlow<List<BlockProfile>>(emptyList())
    val profiles: StateFlow<List<BlockProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow(
        BlockProfile(
            id = "study_mode",
            name = "Study Mode",
            icon = "📚",
            description = "Blocks social feeds, short videos, and web distractions",
            packages = DEFAULT_STUDY_PKGS
        )
    )
    val activeProfile: StateFlow<BlockProfile> = _activeProfile.asStateFlow()

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs

        val studyPkgs = loadProfilePackages(prefs, "study_mode", DEFAULT_STUDY_PKGS)
        val codingPkgs = loadProfilePackages(prefs, "coding_work", DEFAULT_CODING_PKGS)
        val lockdownPkgs = loadProfilePackages(prefs, "strict_lockdown", DEFAULT_LOCKDOWN_PKGS)

        val builtInProfiles = mutableListOf(
            BlockProfile(
                id = "study_mode",
                name = "Study Mode",
                icon = "📚",
                description = "Blocks social feeds, short videos, and web distractions",
                packages = studyPkgs
            ),
            BlockProfile(
                id = "coding_work",
                name = "Coding & Work",
                icon = "💻",
                description = "Blocks entertainment & feeds, preserves developer tools",
                packages = codingPkgs
            ),
            BlockProfile(
                id = "strict_lockdown",
                name = "Strict Lockdown",
                icon = "🔒",
                description = "Maximum restriction across all non-essential apps",
                packages = lockdownPkgs
            )
        )

        // Load custom profiles
        val customNames = prefs.getStringSet(KEY_CUSTOM_PROFILES_LIST, emptySet()) ?: emptySet()
        customNames.forEach { profileId ->
            val name = prefs.getString("profile_name_$profileId", profileId) ?: profileId
            val icon = prefs.getString("profile_icon_$profileId", "🎯") ?: "🎯"
            val pkgs = loadProfilePackages(prefs, profileId, emptySet())
            builtInProfiles.add(
                BlockProfile(
                    id = profileId,
                    name = name,
                    icon = icon,
                    description = "Custom User Block Profile",
                    packages = pkgs,
                    isCustom = true
                )
            )
        }

        _profiles.value = builtInProfiles

        val savedActiveId = prefs.getString(KEY_ACTIVE_PROFILE_ID, "study_mode") ?: "study_mode"
        val active = builtInProfiles.find { it.id == savedActiveId } ?: builtInProfiles.first()
        _activeProfile.value = active

        // Sync with BlockerManager
        BlockerManager.setBlockedPackages(active.packages)
    }

    fun selectProfile(profileId: String) {
        val found = _profiles.value.find { it.id == profileId } ?: return
        _activeProfile.value = found
        BlockerManager.setBlockedPackages(found.packages)

        sharedPreferences?.edit()?.putString(KEY_ACTIVE_PROFILE_ID, profileId)?.apply()
    }

    fun togglePackageInActiveProfile(packageName: String) {
        val active = _activeProfile.value
        val updatedPackages = active.packages.toMutableSet()
        if (updatedPackages.contains(packageName)) {
            updatedPackages.remove(packageName)
        } else {
            updatedPackages.add(packageName)
        }

        val updatedProfile = active.copy(packages = updatedPackages)
        _activeProfile.value = updatedProfile

        // Update in list
        _profiles.value = _profiles.value.map {
            if (it.id == updatedProfile.id) updatedProfile else it
        }

        // Sync to BlockerManager
        BlockerManager.setBlockedPackages(updatedPackages)

        // Persist
        sharedPreferences?.edit()?.apply {
            putStringSet("$PREFIX_PROFILE_PKGS${active.id}", HashSet(updatedPackages))
            apply()
        }
    }

    fun createCustomProfile(name: String, icon: String = "⚡"): BlockProfile {
        val id = "custom_" + System.currentTimeMillis()
        val initialPkgs = _activeProfile.value.packages
        val newProfile = BlockProfile(
            id = id,
            name = name,
            icon = icon,
            description = "Custom block profile: $name",
            packages = initialPkgs,
            isCustom = true
        )

        val updatedList = _profiles.value + newProfile
        _profiles.value = updatedList
        _activeProfile.value = newProfile
        BlockerManager.setBlockedPackages(newProfile.packages)

        val prefs = sharedPreferences
        val currentCustom = prefs?.getStringSet(KEY_CUSTOM_PROFILES_LIST, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentCustom.add(id)

        prefs?.edit()?.apply {
            putString(KEY_ACTIVE_PROFILE_ID, id)
            putString("profile_name_$id", name)
            putString("profile_icon_$id", icon)
            putStringSet("$PREFIX_PROFILE_PKGS$id", HashSet(initialPkgs))
            putStringSet(KEY_CUSTOM_PROFILES_LIST, HashSet(currentCustom))
            apply()
        }

        return newProfile
    }

    fun resetToDefaults() {
        val prefs = sharedPreferences ?: return
        prefs.edit().clear().apply()
        val defaultProfile = BlockProfile(
            id = "study_mode",
            name = "Study Mode",
            icon = "📚",
            description = "Blocks social feeds, short videos, and web distractions",
            packages = DEFAULT_STUDY_PKGS
        )
        _activeProfile.value = defaultProfile
        _profiles.value = listOf(
            defaultProfile,
            BlockProfile(
                id = "coding_work",
                name = "Coding & Work",
                icon = "💻",
                description = "Blocks entertainment & feeds, preserves developer tools",
                packages = DEFAULT_CODING_PKGS
            ),
            BlockProfile(
                id = "strict_lockdown",
                name = "Strict Lockdown",
                icon = "🔒",
                description = "Maximum friction. Blocks everything except system dialer",
                packages = DEFAULT_LOCKDOWN_PKGS
            )
        )
        BlockerManager.setBlockedPackages(defaultProfile.packages)
    }

    private fun loadProfilePackages(
        prefs: SharedPreferences,
        profileId: String,
        defaultPkgs: Set<String>
    ): Set<String> {
        val saved = prefs.getStringSet("$PREFIX_PROFILE_PKGS$profileId", null)
        return saved?.toSet() ?: defaultPkgs
    }
}
