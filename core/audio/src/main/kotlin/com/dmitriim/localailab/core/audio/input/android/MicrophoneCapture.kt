package com.dmitriim.localailab.core.audio.input.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.dmitriim.localailab.core.audio.input.model.AudioLevel
import com.dmitriim.localailab.core.audio.input.model.PcmAudioInput
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Sole owner of the active [AudioRecord] session. */
@Inject
@SingleIn(AppScope::class)
class MicrophoneCapture(private val application: Application) {
    private val lock = Any()

    @Volatile private var activeRecord: AudioRecord? = null

    @Volatile private var capturing = false

    @SuppressLint("MissingPermission") // Guarded by the explicit runtime check immediately below.
    suspend fun capture(
        output: File,
        sampleRateHz: Int,
        onLevel: (AudioLevel) -> Unit,
    ): PcmAudioInput =
        withContext(Dispatchers.IO) {
            val minBufferBytes = requiredBufferSize(sampleRateHz)
            Log.i(
                TAG,
                "Microphone capture preparing: sampleRateHz=$sampleRateHz, minBufferBytes=$minBufferBytes",
            )
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferBytes.coerceAtLeast(sampleRateHz / 5 * PCM16_BYTES),
            )
            require(record.state == AudioRecord.STATE_INITIALIZED) {
                "Android could not initialize the microphone."
            }
            synchronized(lock) {
                check(activeRecord == null) { "A recording session is already active." }
                activeRecord = record
                capturing = true
            }
            val samples = ShortArray((minBufferBytes / PCM16_BYTES).coerceAtLeast(1))
            var frameCount = 0L
            try {
                record.startRecording()
                Log.i(TAG, "Microphone capture started: audioSource=VOICE_RECOGNITION")
                BufferedOutputStream(FileOutputStream(output)).use { stream ->
                    while (capturing) {
                        coroutineContext.ensureActive()
                        val read = record.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                        when {
                            read > 0 -> {
                                stream.writePcm16(samples, read)
                                frameCount += read
                                onLevel(
                                    samples.toAudioLevel(read, frameCount * 1_000 / sampleRateHz),
                                )
                            }
                            read == AudioRecord.ERROR_DEAD_OBJECT -> error(
                                "The microphone became unavailable.",
                            )
                            read < 0 && capturing -> error(
                                "Microphone capture failed (code $read).",
                            )
                        }
                    }
                }
                require(frameCount > 0) { "No audio was captured." }
                Log.i(
                    TAG,
                    "Microphone capture completed: frames=$frameCount, durationMs=${frameCount * 1_000 / sampleRateHz}",
                )
                PcmAudioInput(
                    output,
                    "Microphone recording",
                    frameCount * 1_000 / sampleRateHz,
                    sampleRateHz,
                    "Live microphone",
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Microphone capture failed: ${error.message}", error)
                output.delete()
                throw error
            } finally {
                runCatching { record.stop() }
                record.release()
                synchronized(lock) {
                    if (activeRecord === record) activeRecord = null
                    capturing = false
                }
                Log.i(TAG, "Microphone capture resources released.")
            }
        }

    fun stop() {
        Log.i(TAG, "Microphone capture stop requested.")
        capturing = false
        activeRecord?.let { record -> runCatching { record.stop() } }
    }

    private fun requiredBufferSize(sampleRateHz: Int): Int {
        check(application.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Microphone permission is required before recording."
        }
        return AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).also { size ->
            require(size > 0) { "This device cannot capture mono PCM audio at $sampleRateHz Hz." }
        }
    }
}

private const val PCM16_BYTES = 2
private const val TAG = "AiP123Stt"

private fun BufferedOutputStream.writePcm16(samples: ShortArray, count: Int) {
    for (index in 0 until count) {
        val value = samples[index].toInt()
        write(value and 0xff)
        write((value ushr 8) and 0xff)
    }
}

private fun ShortArray.toAudioLevel(count: Int, elapsedMs: Long): AudioLevel {
    var peak = 0f
    var squares = 0.0
    for (index in 0 until count) {
        val value = this[index] / 32768f
        peak = maxOf(peak, kotlin.math.abs(value))
        squares += value * value
    }
    return AudioLevel(elapsedMs, peak, sqrt(squares / count).toFloat())
}
