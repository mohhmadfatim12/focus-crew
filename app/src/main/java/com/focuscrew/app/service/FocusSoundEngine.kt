package com.focuscrew.app.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sound presets for deep focus and dopamine calm.
 */
enum class FocusSoundType(val title: String, val description: String, val icon: String) {
    OFF("Silent", "No background sound", "🔇"),
    BINAURAL_ALPHA("Binaural Alpha (10Hz)", "Promotes calm alertness & study flow", "🧠"),
    BINAURAL_GAMMA("Binaural Focus (40Hz)", "Peak cognitive alertness & memorization", "⚡"),
    BROWN_NOISE("Deep Brown Noise", "Warm rumble that drowns out ADHD chatter", "🌊"),
    SOFT_RAIN("Gentle Rain Ambience", "Soothing drops to ease tension & anxiety", "🌧️")
}

/**
 * FocusSoundEngine — 100% Offline Audio Tone & Noise Synthesizer.
 *
 * Uses native AudioTrack in stereo mode.
 * Zero external audio files, zero network calls, zero battery drain.
 */
object FocusSoundEngine {

    private const val SAMPLE_RATE = 44100
    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _activeSound = MutableStateFlow(FocusSoundType.OFF)
    val activeSound: StateFlow<FocusSoundType> = _activeSound.asStateFlow()

    private val _volume = MutableStateFlow(0.7f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    fun setSound(type: FocusSoundType) {
        if (_activeSound.value == type) return
        stop()

        _activeSound.value = type
        if (type != FocusSoundType.OFF) {
            startSynthesis(type)
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        audioTrack?.setVolume(clamped)
    }

    fun stop() {
        synthesisJob?.cancel()
        synthesisJob = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        _activeSound.value = FocusSoundType.OFF
    }

    private fun startSynthesis(type: FocusSoundType) {
        synthesisJob = scope.launch {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBufSize.coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.setVolume(_volume.value)
            track.play()
            audioTrack = track

            val buffer = ShortArray(bufferSize / 2) // 16-bit PCM = 2 bytes per sample
            var sampleIndex = 0L
            var brownLastLeft = 0.0
            var brownLastRight = 0.0

            while (isActive) {
                for (i in 0 until buffer.size step 2) {
                    val t = sampleIndex.toDouble() / SAMPLE_RATE

                    val (leftSample, rightSample) = when (type) {
                        FocusSoundType.BINAURAL_ALPHA -> {
                            // Left ear: 200 Hz, Right ear: 210 Hz (difference = 10 Hz alpha wave)
                            val left = sin(2.0 * PI * 200.0 * t) * 0.4
                            val right = sin(2.0 * PI * 210.0 * t) * 0.4
                            left to right
                        }
                        FocusSoundType.BINAURAL_GAMMA -> {
                            // Left ear: 200 Hz, Right ear: 240 Hz (difference = 40 Hz gamma peak focus)
                            val left = sin(2.0 * PI * 200.0 * t) * 0.4
                            val right = sin(2.0 * PI * 240.0 * t) * 0.4
                            left to right
                        }
                        FocusSoundType.BROWN_NOISE -> {
                            // Integration of white noise (brown noise filter)
                            val whiteLeft = (Random.nextDouble() * 2.0 - 1.0)
                            val whiteRight = (Random.nextDouble() * 2.0 - 1.0)
                            brownLastLeft = (brownLastLeft + (0.03 * whiteLeft)) / 1.025
                            brownLastRight = (brownLastRight + (0.03 * whiteRight)) / 1.025
                            (brownLastLeft * 3.5).coerceIn(-1.0, 1.0) to (brownLastRight * 3.5).coerceIn(-1.0, 1.0)
                        }
                        FocusSoundType.SOFT_RAIN -> {
                            // Modulated pink/brown noise mimicking gentle raindrops
                            val whiteL = (Random.nextDouble() * 2.0 - 1.0)
                            val whiteR = (Random.nextDouble() * 2.0 - 1.0)
                            brownLastLeft = (brownLastLeft + (0.05 * whiteL)) / 1.04
                            brownLastRight = (brownLastRight + (0.05 * whiteR)) / 1.04
                            val rainMod = (sin(2.0 * PI * 0.2 * t) * 0.15) + 0.85
                            (brownLastLeft * 2.5 * rainMod).coerceIn(-1.0, 1.0) to (brownLastRight * 2.5 * rainMod).coerceIn(-1.0, 1.0)
                        }
                        FocusSoundType.OFF -> 0.0 to 0.0
                    }

                    buffer[i] = (leftSample * Short.MAX_VALUE).toInt().toShort()
                    buffer[i + 1] = (rightSample * Short.MAX_VALUE).toInt().toShort()
                    sampleIndex++
                }

                track.write(buffer, 0, buffer.size)
            }
        }
    }
}
