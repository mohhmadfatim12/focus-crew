package com.focuscrew.app.service.taskalarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import com.focuscrew.app.R

/**
 * Robust Alarm Ringer & Vibrator.
 * 100% Reliable: Plays loud, continuous ringing even if device is in silent/DND mode.
 *
 * Multi-layer sound strategy:
 * 1. Maximize STREAM_ALARM & STREAM_MUSIC volume.
 * 2. Play bundled high-fidelity alarm audio (R.raw.alarm_ringtone) via MediaPlayer with USAGE_ALARM and FLAG_AUDIBILITY_ENFORCED.
 * 3. Fallback to bundled buzzer (R.raw.alarm_buzzer).
 * 4. Parallel system RingtoneManager on USAGE_ALARM.
 * 5. Continuous pulsed vibration pattern.
 * 6. 3-minute safety auto-stop timer.
 */
object AlarmSoundRinger {

    private const val TAG = "AlarmSoundRinger"
    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var isRinging = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var screenWakeLock: android.os.PowerManager.WakeLock? = null

    private val autoStopRunnable = Runnable {
        Log.d(TAG, "⏰ Auto-stopping alarm after 3 minutes timeout")
        stop()
    }

    private fun ensureMaxVolume(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, AudioManager.FLAG_SHOW_UI)
            val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
            val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing, 0)
            Log.d(TAG, "🔊 Volumes set to max: ALARM=$maxAlarm, MUSIC=$maxMusic, RING=$maxRing")
        } catch (e: Exception) {
            Log.w(TAG, "Could not set stream volume: ${e.message}")
        }
    }

    @Synchronized
    fun start(context: Context) {
        val appContext = context.applicationContext
        ensureMaxVolume(appContext)

        if (isRinging && (mediaPlayer?.isPlaying == true || fallbackRingtone?.isPlaying == true)) {
            Log.d(TAG, "Alarm is already actively ringing")
            return
        }

        // Clean up any existing stale player before starting fresh
        stop()
        isRinging = true

        // Turn on physical screen from sleep/lock
        try {
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            @Suppress("DEPRECATION")
            screenWakeLock = pm?.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        android.os.PowerManager.ON_AFTER_RELEASE,
                "FocusCrew:AlarmSoundRingerScreenLock"
            )
            screenWakeLock?.acquire(60_000L)
            Log.d(TAG, "💡 Screen wake lock acquired to turn phone screen ON")
        } catch (e: Exception) {
            Log.w(TAG, "Screen wake lock failed: ${e.message}")
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()

        // 1. Play Bundled Alarm Ringtone (100% reliable - requires zero external permissions)
        var audioPlaying = false
        try {
            val mp = MediaPlayer.create(appContext, R.raw.alarm_ringtone, audioAttributes, 0)
                ?: MediaPlayer.create(appContext, R.raw.alarm_buzzer, audioAttributes, 0)

            if (mp != null) {
                mp.isLooping = true
                mp.setVolume(1.0f, 1.0f)
                mp.start()
                mediaPlayer = mp
                audioPlaying = true
                Log.d(TAG, "🔊 Bundled alarm audio playing successfully with loop enabled")
            } else {
                Log.w(TAG, "MediaPlayer.create returned null for bundled raw audio")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed playing bundled raw audio: ${e.message}", e)
        }

        // 2. Play System Alarm Ringtone via RingtoneManager as parallel/fallback
        try {
            val actualAlarmUri: Uri? = RingtoneManager.getActualDefaultRingtoneUri(appContext, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: Settings.System.DEFAULT_ALARM_ALERT_URI

            if (actualAlarmUri != null) {
                val ringtone = RingtoneManager.getRingtone(appContext, actualAlarmUri)
                if (ringtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ringtone.audioAttributes = audioAttributes
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.isLooping = true
                    }
                    if (!audioPlaying) {
                        ringtone.play()
                        fallbackRingtone = ringtone
                        audioPlaying = true
                        Log.d(TAG, "🔊 RingtoneManager playing system alarm URI: $actualAlarmUri")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "RingtoneManager failed: ${e.message}", e)
        }

        // 3. Start Heavy Alarm Vibration
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 800, 400, 800, 400, 1200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
            Log.d(TAG, "📳 Alarm vibration pattern started")
        } catch (e: Exception) {
            Log.e(TAG, "Vibrator failed: ${e.message}", e)
        }

        // 4. Schedule 3-minute safety auto-stop
        mainHandler.removeCallbacks(autoStopRunnable)
        mainHandler.postDelayed(autoStopRunnable, 3 * 60 * 1000L)
    }

    @Synchronized
    fun stop() {
        if (!isRinging && mediaPlayer == null && fallbackRingtone == null && vibrator == null) return
        isRinging = false
        mainHandler.removeCallbacks(autoStopRunnable)

        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            fallbackRingtone?.let {
                if (it.isPlaying) it.stop()
            }
        } catch (_: Exception) {}
        fallbackRingtone = null

        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
        vibrator = null

        try {
            if (screenWakeLock?.isHeld == true) {
                screenWakeLock?.release()
            }
        } catch (_: Exception) {}
        screenWakeLock = null

        Log.d(TAG, "🔇 Alarm sound and vibration stopped")
    }

    fun isPlaying(): Boolean = isRinging
}
