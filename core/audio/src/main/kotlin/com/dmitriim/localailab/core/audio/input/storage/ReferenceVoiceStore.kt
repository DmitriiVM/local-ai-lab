package com.dmitriim.localailab.core.audio.input.storage

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import com.dmitriim.localailab.core.audio.input.android.MicrophoneCapture
import com.dmitriim.localailab.core.audio.input.android.PlatformAudioDecoder
import com.dmitriim.localailab.core.audio.input.model.AudioLevel
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReferenceVoice(
    val id: String,
    val displayName: String,
    val durationMs: Long,
    val createdAtEpochMs: Long,
    val sourceDescription: String,
    val pcmFilePath: String,
    val sampleRateHz: Int = 24_000,
)

/** Persistent app-private normalized PCM reference voices. */
@Inject
@SingleIn(AppScope::class)
class ReferenceVoiceStore(
    application: Application,
    private val microphoneCapture: MicrophoneCapture,
    private val decoder: PlatformAudioDecoder,
) {
    private val directory = File(application.filesDir, "reference-voices").apply { mkdirs() }
    private val mutableVoices = MutableStateFlow(loadVoices())
    val voices: StateFlow<List<ReferenceVoice>> = mutableVoices.asStateFlow()
    private val contentResolver = application.contentResolver

    suspend fun capture(onLevel: (AudioLevel) -> Unit): ReferenceVoice {
        val temporary = File(directory, ".capture-${UUID.randomUUID()}.pcm")
        val input = microphoneCapture.capture(temporary, REFERENCE_SAMPLE_RATE_HZ) { level ->
            onLevel(level)
            if (level.elapsedMs >= MAX_REFERENCE_DURATION_MS) microphoneCapture.stop()
        }
        require(input.durationMs >= MIN_REFERENCE_DURATION_MS) {
            temporary.delete()
            "Record at least 5 seconds of clear speech."
        }
        return persist(
            temporary = temporary,
            displayName = "Recorded voice ${nextDisplayNumber()}",
            durationMs = input.durationMs.coerceAtMost(MAX_REFERENCE_DURATION_MS),
            sourceDescription = "Microphone recording",
        )
    }

    fun stopCapture() = microphoneCapture.stop()

    suspend fun importAudio(uri: Uri): ReferenceVoice {
        val temporary = File(directory, ".import-${UUID.randomUUID()}.pcm")
        return try {
            val decoded = decoder.decodeToMonoPcm(uri, temporary, REFERENCE_SAMPLE_RATE_HZ)
            val durationMs = decoded.frames * 1_000 / REFERENCE_SAMPLE_RATE_HZ
            require(durationMs >= MIN_REFERENCE_DURATION_MS) {
                "Reference audio must contain at least 5 seconds of speech."
            }
            persist(
                temporary = temporary,
                displayName = displayName(uri).substringBeforeLast('.').ifBlank {
                    "Imported voice"
                },
                durationMs = durationMs.coerceAtMost(MAX_REFERENCE_DURATION_MS),
                sourceDescription = "Imported ${decoded.mimeType}",
            )
        } catch (error: Throwable) {
            temporary.delete()
            if (error is IllegalArgumentException) throw error
            throw IllegalArgumentException(
                "Could not decode this audio. Choose a device-supported WAV, MP3, M4A/AAC, or OGG/Opus file.",
                error,
            )
        }
    }

    fun resolve(id: String): ReferenceVoice? = mutableVoices.value.firstOrNull { it.id == id && File(it.pcmFilePath).isFile }

    fun delete(id: String) {
        val voice = mutableVoices.value.firstOrNull { it.id == id } ?: return
        File(voice.pcmFilePath).delete()
        metadataFile(id).delete()
        mutableVoices.value = loadVoices()
    }

    private fun persist(
        temporary: File,
        displayName: String,
        durationMs: Long,
        sourceDescription: String,
    ): ReferenceVoice {
        val id = UUID.randomUUID().toString()
        val pcm = File(directory, "$id.pcm")
        val retainedBytes = ReferenceVoiceMetadataCodec.retainedPcmBytes(durationMs)
        java.io.RandomAccessFile(temporary, "rw").use { it.setLength(retainedBytes) }
        if (!temporary.renameTo(pcm)) {
            temporary.copyTo(pcm, overwrite = false)
            temporary.delete()
        }
        val voice = ReferenceVoice(
            id = id,
            displayName = displayName.take(80),
            durationMs = ReferenceVoiceMetadataCodec.durationMs(pcm.length()),
            createdAtEpochMs = System.currentTimeMillis(),
            sourceDescription = sourceDescription,
            pcmFilePath = pcm.absolutePath,
        )
        ReferenceVoiceMetadataCodec.write(metadataFile(id), voice)
        mutableVoices.value = loadVoices()
        return voice
    }

    private fun loadVoices(): List<ReferenceVoice> = directory.listFiles { file ->
        file.extension ==
            "properties"
    }
        .orEmpty()
        .mapNotNull { metadata -> ReferenceVoiceMetadataCodec.read(directory, metadata) }
        .sortedByDescending(ReferenceVoice::createdAtEpochMs)

    private fun metadataFile(id: String) = File(directory, "$id.properties")

    private fun nextDisplayNumber(): Int = mutableVoices.value.count { it.displayName.startsWith("Recorded voice") } + 1

    private fun displayName(uri: Uri): String = contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: uri.lastPathSegment?.substringAfterLast('/')
        ?: "Imported voice"

    companion object {
        const val REFERENCE_SAMPLE_RATE_HZ = 24_000
        const val MIN_REFERENCE_DURATION_MS = 5_000L
        const val MAX_REFERENCE_DURATION_MS = 10_000L
    }
}
