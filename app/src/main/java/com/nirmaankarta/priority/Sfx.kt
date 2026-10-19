package com.nirmaankarta.priority

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Procedural sound effects: every sound is synthesised from oscillators with an
 * exponential decay envelope and played through AudioTrack. No audio assets.
 */
object Sfx {
    private const val SR = 22050
    private const val SINE = 0
    private const val SQUARE = 1
    private const val TRIANGLE = 2

    private class Tone(
        val start: Float,
        val dur: Float,
        val f0: Float,
        val f1: Float = f0,
        val gain: Float = 0.3f,
        val wave: Int = SINE,
        val lowpass: Float = 0f,
    )

    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val cache = HashMap<String, ShortArray>()

    fun play(q: Quadrant, add: Boolean) {
        worker.execute {
            runCatching {
                val pcm = cache.getOrPut("${q.name}$add") { render(tones(q, add)) }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SR)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                val ms = pcm.size * 1000L / SR + 250
                main.postDelayed({ runCatching { track.release() } }, ms)
            }
        }
    }

    private fun tones(q: Quadrant, add: Boolean): List<Tone> = when (q) {
        Quadrant.Q1 -> if (add) listOf(
            // punchy alert ping 520 -> 680 Hz
            Tone(0f, 0.14f, 520f, 680f, 0.35f, TRIANGLE),
            Tone(0.09f, 0.18f, 680f, 680f, 0.3f, TRIANGLE),
        ) else listOf(
            // victory fanfare C5 E5 G5 C6
            Tone(0.00f, 0.35f, 523.25f, gain = 0.25f, wave = TRIANGLE),
            Tone(0.11f, 0.35f, 659.25f, gain = 0.25f, wave = TRIANGLE),
            Tone(0.22f, 0.35f, 783.99f, gain = 0.25f, wave = TRIANGLE),
            Tone(0.33f, 0.80f, 1046.50f, gain = 0.28f, wave = TRIANGLE),
        )

        Quadrant.Q2 -> if (add) listOf(
            // calm dual-sine chord
            Tone(0f, 0.65f, 440f, gain = 0.22f),
            Tone(0f, 0.65f, 554f, gain = 0.22f),
        ) else listOf(
            // singing-bowl resonance
            Tone(0.00f, 1.6f, 392f, gain = 0.18f),
            Tone(0.06f, 1.6f, 493f, gain = 0.16f),
            Tone(0.12f, 1.6f, 587f, gain = 0.16f),
            Tone(0.18f, 1.6f, 880f, gain = 0.10f),
        )

        Quadrant.Q3 -> if (add) listOf(
            // ascending whoosh-pop 260 -> 740 Hz
            Tone(0f, 0.20f, 260f, 740f, 0.3f, TRIANGLE),
            Tone(0.19f, 0.08f, 740f, gain = 0.2f),
        ) else listOf(
            // double click / handoff pop
            Tone(0.00f, 0.05f, 1400f, 900f, 0.3f, SQUARE, 4000f),
            Tone(0.09f, 0.07f, 1000f, 700f, 0.3f, SQUARE, 4000f),
        )

        Quadrant.Q4 -> if (add) listOf(
            // muted low-pass woodblock thud
            Tone(0f, 0.16f, 160f, 120f, 0.7f, SQUARE, 500f),
        ) else listOf(
            // descending poof 420 -> 80 Hz
            Tone(0f, 0.40f, 420f, 80f, 0.35f),
        )
    }

    private fun render(tones: List<Tone>): ShortArray {
        val total = tones.maxOf { it.start + it.dur }
        val buf = FloatArray((total * SR).toInt() + 1)
        for (t in tones) {
            val n = (t.dur * SR).toInt()
            val s0 = (t.start * SR).toInt()
            var phase = 0.0
            var lp = 0f
            val alpha = if (t.lowpass > 0f) {
                val x = (2.0 * PI * t.lowpass / SR).toFloat(); x / (x + 1f)
            } else 1f
            for (i in 0 until n) {
                val p = i.toFloat() / n
                val f = if (t.f1 == t.f0) t.f0 else t.f0 * (t.f1 / t.f0).pow(p)
                phase += 2.0 * PI * f / SR
                val raw: Double = when (t.wave) {
                    SQUARE -> if (sin(phase) >= 0) 1.0 else -1.0
                    TRIANGLE -> 2.0 / PI * asin(sin(phase))
                    else -> sin(phase)
                }
                var v = raw.toFloat()
                if (t.lowpass > 0f) { lp += alpha * (v - lp); v = lp }
                val attack = min(1f, i / (0.005f * SR))
                val fade = min(1f, (1f - p) / 0.05f)
                val env = attack * fade * exp(-5f * p) // exponential decay
                val idx = s0 + i
                if (idx < buf.size) buf[idx] += v * t.gain * env
            }
        }
        return ShortArray(buf.size) { (buf[it].coerceIn(-1f, 1f) * 32767f).toInt().toShort() }
    }
}
