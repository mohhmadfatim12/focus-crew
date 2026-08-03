package com.focuscrew.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

/**
 * BinauralAudioGenerator — Generates a soothing 432Hz sine wave tone
 * mathematically in real-time using Android AudioTrack.
 *
 * 100% Offline, zero external sound files, zero network calls.
 * Used during the 3-Round Urge Destroyer to activate parasympathetic calm.
 */
object BinauralAudioGenerator {

    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false
    private var playbackThread: Thread? = null

    fun play432Hz() {
        if (isPlaying) return
        isPlaying = true

        playbackThread = Thread {
            try {
                val sampleRate = 44100
                val freq = 432.0 // Scientific frequency for parasympathetic grounding
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

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
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                val buffer = ShortArray(bufferSize / 2)
                var angle = 0.0
                val angleIncrement = (2.0 * Math.PI * freq) / sampleRate

                while (isPlaying) {
                    for (i in buffer.indices) {
                        // Soft 20% volume limit for smooth, soothing background sound
                        val sample = (sin(angle) * Short.MAX_VALUE * 0.20).toInt()
                        buffer[i] = sample.toShort()
                        angle += angleIncrement
                        if (angle > 2.0 * Math.PI) {
                            angle -= 2.0 * Math.PI
                        }
                    }
                    track.write(buffer, 0, buffer.size)
                }

                track.stop()
                track.release()
            } catch (_: Exception) {
                // Audio track failure handled gracefully
            }
        }.apply {
            name = "FocusCrew-432Hz-Tone"
            isDaemon = true
            start()
        }
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        playbackThread = null
    }

    fun isAudioPlaying(): Boolean = isPlaying
}
