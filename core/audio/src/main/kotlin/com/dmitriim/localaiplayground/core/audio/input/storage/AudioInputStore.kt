package com.dmitriim.localaiplayground.core.audio.input.storage

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import com.dmitriim.localaiplayground.core.audio.input.android.MicrophoneCapture
import com.dmitriim.localaiplayground.core.audio.input.android.PlatformAudioDecoder
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.audio.input.model.AudioLevel
import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import com.dmitriim.localaiplayground.core.audio.input.model.STT_SAMPLE_RATE_HZ
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.util.UUID

/** Owns session-scoped temporary STT input and delegates each audio concern. */
@Inject
@SingleIn(AppScope::class)
class AudioInputStore(private val application: Application) {
    private val directory = File(application.cacheDir, "stt-inputs")
    private val microphoneCapture = MicrophoneCapture(application)
    private val decoder = PlatformAudioDecoder(application)

    init {
        directory.mkdirs()
        // Cache is session-only. A new process starts without recoverable input.
        directory.listFiles()?.forEach(File::delete)
    }

    suspend fun capture(sampleRateHz: Int = STT_SAMPLE_RATE_HZ, onLevel: (AudioLevel) -> Unit): PcmAudioInput =
        microphoneCapture.capture(newInputFile(), sampleRateHz, onLevel)

    fun stopCapture() = microphoneCapture.stop()

    suspend fun importAudio(uri: Uri, targetSampleRateHz: Int = STT_SAMPLE_RATE_HZ): PcmAudioInput {
        val output = newInputFile()
        return try {
            val decoded = decoder.decodeToMonoPcm(uri, output, targetSampleRateHz)
            require(decoded.frames > 0) { "The selected file contains no decodable audio." }
            PcmAudioInput(
                file = output,
                displayName = displayName(uri),
                durationMs = decoded.frames * 1_000 / targetSampleRateHz,
                sampleRateHz = targetSampleRateHz,
                sourceDescription = "Imported ${decoded.mimeType}",
            )
        } catch (error: Throwable) {
            output.delete()
            throw IllegalArgumentException(
                "Could not decode this audio file. Choose a device-supported WAV, MP3, M4A/AAC, or OGG/Opus file.",
                error,
            )
        }
    }

    suspend fun forEachSegment(input: PcmAudioInput, onSegment: (FloatArray) -> Unit) =
        PcmSegmentReader.forEachSegment(input, onSegment)

    fun clear(input: PcmAudioInput?) {
        input?.file?.takeIf { it.parentFile == directory }?.delete()
    }

    fun clearAll() {
        microphoneCapture.stop()
        directory.listFiles()?.forEach(File::delete)
    }

    private fun newInputFile(): File = File(directory, "stt-${UUID.randomUUID()}.pcm")

    private fun displayName(uri: Uri): String = application.contentResolver.query(
        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Imported audio"
}
