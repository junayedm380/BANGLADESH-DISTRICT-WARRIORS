package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.game.inventory.WeaponCategory
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Zero-asset procedural PCM audio synthesizer for weapon shots, melee swings, hit markers, and vehicle engines.
 * Respects mute setting and safely no-ops if audio hardware is unavailable (e.g., JVM tests).
 */
class SoundEffectsEngine {
    @Volatile
    var enabled: Boolean = true

    fun playWeaponShot(category: WeaponCategory, suppressed: Boolean) {
        if (!enabled) return
        val durationMs = when {
            suppressed -> 65
            category == WeaponCategory.SNIPER || category == WeaponCategory.SHOTGUN -> 160
            category == WeaponCategory.MELEE -> 90
            else -> 95
        }
        val baseFreq = when {
            suppressed -> 520f
            category == WeaponCategory.SNIPER -> 140f
            category == WeaponCategory.SHOTGUN -> 165f
            category == WeaponCategory.SMG -> 340f
            category == WeaponCategory.MELEE -> 260f
            else -> 230f
        }
        val noiseMix = if (category == WeaponCategory.MELEE) 0.25f else if (suppressed) 0.35f else 0.72f
        val gain = if (suppressed) 0.25f else 0.48f
        playSynthBurst(durationMs, baseFreq, noiseMix, gain)
    }

    fun playHitMarker() {
        if (!enabled) return
        playSynthBurst(45, 1180f, 0.1f, 0.35f)
    }

    fun playVehiclePulse(speedRatio: Float) {
        if (!enabled) return
        val freq = 72f + speedRatio.coerceIn(0f, 1f) * 110f
        playSynthBurst(70, freq, 0.18f, 0.22f)
    }

    private fun playSynthBurst(durationMs: Int, freqHz: Float, noiseMix: Float, gain: Float) {
        thread(isDaemon = true) {
            try {
                val sampleRate = 22050
                val count = (sampleRate * durationMs) / 1000
                if (count <= 0) return@thread
                val pcm = ShortArray(count)
                val rng = Random(System.nanoTime())
                for (i in 0 until count) {
                    val t = i.toFloat() / sampleRate
                    val env = exp((-t * (1000f / durationMs) * 4.2f).toDouble()).toFloat()
                    val tone = sin(2.0 * PI * freqHz * t).toFloat()
                    val noise = (rng.nextFloat() * 2f - 1f)
                    val sample = ((tone * (1f - noiseMix) + noise * noiseMix) * env * gain).coerceIn(-0.95f, 0.95f)
                    pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf <= 0) return@thread
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, pcm.size * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 25L)
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Safe fallback on headless/JVM test environments
            }
        }
    }
}
