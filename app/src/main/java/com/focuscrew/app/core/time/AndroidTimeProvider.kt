package com.focuscrew.app.core.time

import android.os.SystemClock

/**
 * Android implementation of TimeProvider using SystemClock.elapsedRealtime().
 */
class AndroidTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
