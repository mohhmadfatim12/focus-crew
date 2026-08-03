package com.focuscrew.app.core.time

/**
 * Pure Kotlin abstraction for time sources.
 * Allows deterministic unit testing without depending on Android SDK clocks.
 */
interface TimeProvider {
    /** Wall-clock time in milliseconds since epoch (e.g., System.currentTimeMillis()). */
    fun currentTimeMillis(): Long

    /** Monotonic time in milliseconds including deep sleep (e.g., SystemClock.elapsedRealtime()). */
    fun elapsedRealtime(): Long
}
