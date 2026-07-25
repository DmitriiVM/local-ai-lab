package com.dmitriim.localaiplayground.core.audio.output.storage

import android.app.Application
import android.net.Uri
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.audio.output.model.GeneratedAudioFile
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Retains exactly one successful generated WAV. A temporary file is never exposed
 * and is replaced atomically only after a complete, valid WAV has been written.
 */
@Inject
@SingleIn(AppScope::class)
class GeneratedAudioStore(private val application: Application) {
    private val directory = File(application.filesDir, DIRECTORY_NAME)
    private val latestFile get() = File(directory, LATEST_NAME)
    private val partialFile get() = File(directory, PARTIAL_NAME)
    private val backupFile get() = File(directory, BACKUP_NAME)

    @Synchronized
    fun saveLatest(samples: FloatArray, sampleRateHz: Int): GeneratedAudioFile {
        require(samples.isNotEmpty()) { "Generated audio is empty." }
        require(sampleRateHz > 0) { "Generated audio has an invalid sample rate." }
        require(directory.exists() || directory.mkdirs()) {
            "Could not create app-private generated-audio storage."
        }
        partialFile.delete()
        writeWave(partialFile, samples, sampleRateHz)
        require(readMetadata(partialFile) != null) { "The generated WAV could not be validated." }
        backupFile.delete()
        if (latestFile.exists()) require(latestFile.renameTo(backupFile)) {
            "Could not preserve the previous generated audio during replacement."
        }
        try {
            if (!partialFile.renameTo(latestFile)) {
                partialFile.copyTo(latestFile, overwrite = true)
                require(partialFile.delete()) { "Could not finish generated-audio cleanup." }
            }
            require(readMetadata(latestFile) != null) { "The retained WAV could not be validated." }
            backupFile.delete()
        } catch (error: Throwable) {
            latestFile.delete()
            if (backupFile.exists()) backupFile.renameTo(latestFile)
            throw error
        }
        return requireNotNull(readMetadata(latestFile))
    }

    @Synchronized
    fun latest(): GeneratedAudioFile? {
        partialFile.delete()
        if (!latestFile.exists() && backupFile.exists()) backupFile.renameTo(latestFile)
        if (latestFile.exists()) backupFile.delete()
        return readMetadata(latestFile)
    }

    fun discardPartial() {
        synchronized(this) { partialFile.delete() }
    }

    /** Clears only app-private retained output; previously exported documents are unaffected. */
    fun clearLatest() {
        synchronized(this) {
            partialFile.delete()
            backupFile.delete()
            latestFile.delete()
        }
    }

    fun export(audio: GeneratedAudioFile, destination: Uri) {
        val source = checkedFile(audio)
        application.contentResolver.openOutputStream(destination, "w")?.use { output ->
            FileInputStream(source).use { input -> input.copyTo(output) }
        } ?: error("Android could not open the selected export destination.")
    }

    fun streamPcm16(audio: GeneratedAudioFile, onChunk: (ByteArray) -> Boolean) {
        val source = checkedFile(audio)
        BufferedInputStream(FileInputStream(source)).use { input ->
            var remaining = audio.sampleCount.toLong() * PCM_BYTES_PER_SAMPLE
            var skipped = 0L
            while (skipped < WAV_HEADER_BYTES) {
                val count = input.skip(WAV_HEADER_BYTES - skipped)
                require(count > 0) { "The generated WAV header is truncated." }
                skipped += count
            }
            val buffer = ByteArray(STREAM_CHUNK_BYTES)
            while (remaining > 0) {
                val requested = minOf(buffer.size.toLong(), remaining).toInt()
                val count = input.read(buffer, 0, requested)
                require(count > 0) { "The generated WAV data is truncated." }
                val chunk = if (count == buffer.size) buffer.copyOf() else buffer.copyOf(count)
                if (!onChunk(chunk)) return
                remaining -= count
            }
        }
    }

    private fun checkedFile(audio: GeneratedAudioFile): File {
        val file = File(audio.filePath)
        require(file.canonicalFile == latestFile.canonicalFile && file.isFile) {
            "The latest generated audio is no longer available."
        }
        return file
    }

    private fun writeWave(file: File, samples: FloatArray, sampleRateHz: Int) {
        val dataBytes = Math.multiplyExact(samples.size, PCM_BYTES_PER_SAMPLE)
        val header = ByteBuffer.allocate(WAV_HEADER_BYTES.toInt()).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataBytes)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRateHz)
            putInt(sampleRateHz * PCM_BYTES_PER_SAMPLE)
            putShort(PCM_BYTES_PER_SAMPLE.toShort())
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataBytes)
        }
        FileOutputStream(file).use { fileOutput ->
            val output = BufferedOutputStream(fileOutput)
            output.write(header.array())
            val pcm = ByteArray(STREAM_CHUNK_BYTES)
            var byteIndex = 0
            samples.forEach { sample ->
                val value = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE)
                    .roundToInt()
                    .toShort()
                    .toInt()
                pcm[byteIndex++] = value.toByte()
                pcm[byteIndex++] = (value shr 8).toByte()
                if (byteIndex == pcm.size) {
                    output.write(pcm)
                    byteIndex = 0
                }
            }
            if (byteIndex > 0) output.write(pcm, 0, byteIndex)
            output.flush()
            fileOutput.fd.sync()
        }
    }

    private fun readMetadata(file: File): GeneratedAudioFile? = runCatching {
        if (!file.isFile || file.length() < WAV_HEADER_BYTES) return null
        RandomAccessFile(file, "r").use { input ->
            require(readAscii(input, 4) == "RIFF")
            input.skipBytes(4)
            require(readAscii(input, 4) == "WAVE")
            require(readAscii(input, 4) == "fmt ")
            require(Integer.reverseBytes(input.readInt()) == 16)
            require(java.lang.Short.reverseBytes(input.readShort()).toInt() == 1)
            require(java.lang.Short.reverseBytes(input.readShort()).toInt() == 1)
            val sampleRate = Integer.reverseBytes(input.readInt())
            input.skipBytes(6)
            require(java.lang.Short.reverseBytes(input.readShort()).toInt() == 16)
            require(readAscii(input, 4) == "data")
            val dataBytes = Integer.reverseBytes(input.readInt())
            require(sampleRate > 0 && dataBytes >= 0 && dataBytes.toLong() + WAV_HEADER_BYTES <= file.length())
            GeneratedAudioFile(
                filePath = file.absolutePath,
                sampleRateHz = sampleRate,
                sampleCount = dataBytes / PCM_BYTES_PER_SAMPLE,
                createdAtEpochMs = file.lastModified(),
            )
        }
    }.getOrNull()

    private fun readAscii(input: RandomAccessFile, count: Int): String {
        val bytes = ByteArray(count)
        input.readFully(bytes)
        return bytes.toString(Charsets.US_ASCII)
    }

    companion object {
        const val DIRECTORY_NAME = "generated-audio"
        private const val LATEST_NAME = "latest.wav"
        private const val PARTIAL_NAME = "latest.partial"
        private const val BACKUP_NAME = "latest.backup"
        private const val PCM_BYTES_PER_SAMPLE = 2
        private const val WAV_HEADER_BYTES = 44L
        private const val STREAM_CHUNK_BYTES = 32 * 1024
    }
}
