package com.dmitriim.localaiplayground.core.audio.input.storage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/** Reads and writes app-private reference voice metadata alongside its normalized PCM file. */
internal object ReferenceVoiceMetadataCodec {
    fun retainedPcmBytes(durationMs: Long): Long = durationMs
        .coerceIn(0L, ReferenceVoiceStore.MAX_REFERENCE_DURATION_MS)
        .times(ReferenceVoiceStore.REFERENCE_SAMPLE_RATE_HZ)
        .div(1_000)
        .times(PCM16_BYTES)

    fun durationMs(pcmBytes: Long): Long = pcmBytes / PCM16_BYTES * 1_000 / ReferenceVoiceStore.REFERENCE_SAMPLE_RATE_HZ

    fun write(metadataFile: File, voice: ReferenceVoice) {
        Properties().apply {
            setProperty("id", voice.id)
            setProperty("displayName", voice.displayName)
            setProperty("durationMs", voice.durationMs.toString())
            setProperty("createdAtEpochMs", voice.createdAtEpochMs.toString())
            setProperty("sourceDescription", voice.sourceDescription)
            FileOutputStream(metadataFile).use { store(it, "Local AI Playground reference voice") }
        }
    }

    fun read(directory: File, metadataFile: File): ReferenceVoice? = runCatching {
        val values = Properties().apply { FileInputStream(metadataFile).use(::load) }
        val id = requireNotNull(values.getProperty("id"))
        require(id.isSafeFileName())
        val pcm = File(directory, "$id.pcm")
        require(pcm.isFile && pcm.canonicalFile.parentFile == directory.canonicalFile)
        ReferenceVoice(
            id = id,
            displayName = requireNotNull(values.getProperty("displayName")),
            durationMs = requireNotNull(values.getProperty("durationMs")).toLong().also { require(it >= 0) },
            createdAtEpochMs = requireNotNull(values.getProperty("createdAtEpochMs")).toLong(),
            sourceDescription = requireNotNull(values.getProperty("sourceDescription")),
            pcmFilePath = pcm.absolutePath,
        )
    }.getOrNull()

    private fun String.isSafeFileName() = isNotBlank() && none { it == '/' || it == '\\' } && this != "." && this != ".."

    private const val PCM16_BYTES = 2L
}
