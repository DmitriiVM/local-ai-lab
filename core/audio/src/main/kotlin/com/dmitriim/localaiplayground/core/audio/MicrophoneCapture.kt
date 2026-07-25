package com.dmitriim.localaiplayground.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Sole owner of the active [AudioRecord] session. */
internal class MicrophoneCapture(private val application: Application) {
    private val lock = Any()
    @Volatile private var activeRecord: AudioRecord? = null
    @Volatile private var capturing = false

    @SuppressLint("MissingPermission") // Guarded by the explicit runtime check immediately below.
    suspend fun capture(output: File, sampleRateHz: Int, onLevel: (AudioLevel) -> Unit): PcmAudioInput =
        withContext(Dispatchers.IO) {
            check(application.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                "Microphone permission is required before recording."
            }
            val minBufferBytes = AudioRecord.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            require(minBufferBytes > 0) { "This device cannot capture mono PCM audio at $sampleRateHz Hz." }
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferBytes.coerceAtLeast(sampleRateHz / 5 * PCM16_BYTES),
            )
            require(record.state == AudioRecord.STATE_INITIALIZED) { "Android could not initialize the microphone." }
            synchronized(lock) {
                check(activeRecord == null) { "A recording session is already active." }
                activeRecord = record
                capturing = true
            }
            val samples = ShortArray((minBufferBytes / PCM16_BYTES).coerceAtLeast(1))
            var frameCount = 0L
            try {
                record.startRecording()
                BufferedOutputStream(FileOutputStream(output)).use { stream ->
                    while (capturing) {
                        coroutineContext.ensureActive()
                        val read = record.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                        when {
                            read > 0 -> {
                                stream.writePcm16(samples, read)
                                frameCount += read
                                onLevel(samples.toAudioLevel(read, frameCount * 1_000 / sampleRateHz))
                            }
                            read == AudioRecord.ERROR_DEAD_OBJECT -> error("The microphone became unavailable.")
                            read < 0 && capturing -> error("Microphone capture failed (code $read).")
                        }
                    }
                }
                require(frameCount > 0) { "No audio was captured." }
                PcmAudioInput(output, "Microphone recording", frameCount * 1_000 / sampleRateHz, sampleRateHz, "Live microphone")
            } catch (error: Throwable) {
                output.delete()
                throw error
            } finally {
                runCatching { record.stop() }
                record.release()
                synchronized(lock) {
                    if (activeRecord === record) activeRecord = null
                    capturing = false
                }
            }
        }

    fun stop() {
        capturing = false
        activeRecord?.let { record -> runCatching { record.stop() } }
    }
}

private const val PCM16_BYTES = 2

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
