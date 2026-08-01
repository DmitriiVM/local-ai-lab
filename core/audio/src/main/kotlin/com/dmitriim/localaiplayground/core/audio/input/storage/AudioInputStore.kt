package com.dmitriim.localaiplayground.core.audio.input.storage

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.dmitriim.localaiplayground.core.audio.input.android.MicrophoneCapture
import com.dmitriim.localaiplayground.core.audio.input.android.PlatformAudioDecoder
import com.dmitriim.localaiplayground.core.audio.input.model.AudioLevel
import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import com.dmitriim.localaiplayground.core.audio.input.model.STT_SAMPLE_RATE_HZ
import com.dmitriim.localaiplayground.core.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.util.UUID

/** Owns session-scoped temporary STT input and delegates each audio concern. */
@Inject
@SingleIn(AppScope::class)
class AudioInputStore(
    private val application: Application,
    private val microphoneCapture: MicrophoneCapture,
    private val decoder: PlatformAudioDecoder,
) {
    private val directory = File(application.cacheDir, "stt-inputs")

    init {
        directory.mkdirs()
        // Cache is session-only. A new process starts without recoverable input.
        directory.listFiles()?.forEach(File::delete)
        Log.i(TAG, "STT input cache initialized: directory=${directory.name}")
    }

    suspend fun capture(sampleRateHz: Int = STT_SAMPLE_RATE_HZ, onLevel: (AudioLevel) -> Unit): PcmAudioInput {
        Log.i(TAG, "STT microphone capture requested: sampleRateHz=$sampleRateHz")
        return microphoneCapture.capture(newInputFile(), sampleRateHz, onLevel)
    }

    fun stopCapture() {
        Log.i(TAG, "STT microphone capture stop forwarded.")
        microphoneCapture.stop()
    }

    suspend fun importAudio(uri: Uri, targetSampleRateHz: Int = STT_SAMPLE_RATE_HZ): PcmAudioInput {
        val output = newInputFile()
        Log.i(TAG, "STT audio import requested: uriScheme=${uri.scheme}, targetRateHz=$targetSampleRateHz")
        return try {
            val decoded = decoder.decodeToMonoPcm(uri, output, targetSampleRateHz)
            require(decoded.frames > 0) { "The selected file contains no decodable audio." }
            PcmAudioInput(
                file = output,
                displayName = displayName(uri),
                durationMs = decoded.frames * 1_000 / targetSampleRateHz,
                sampleRateHz = targetSampleRateHz,
                sourceDescription = "Imported ${decoded.mimeType}",
            ).also { input ->
                Log.i(TAG, "STT audio import completed: durationMs=${input.durationMs}, sampleRateHz=${input.sampleRateHz}, frames=${decoded.frames}")
            }
        } catch (error: Throwable) {
            Log.e(TAG, "STT audio import failed: ${error.message}", error)
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
        input?.file?.takeIf { it.parentFile == directory }?.let { file ->
            if (file.delete()) Log.i(TAG, "STT input cleared: source=${input.sourceDescription}")
        }
    }

    fun clearAll() {
        Log.i(TAG, "STT input cache clear-all requested.")
        microphoneCapture.stop()
        directory.listFiles()?.forEach(File::delete)
    }

    private fun newInputFile(): File = File(directory, "stt-${UUID.randomUUID()}.pcm")

    private fun displayName(uri: Uri): String = application.contentResolver.query(
        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Imported audio"

    private companion object {
        const val TAG = "AiP123Stt"
    }
}
