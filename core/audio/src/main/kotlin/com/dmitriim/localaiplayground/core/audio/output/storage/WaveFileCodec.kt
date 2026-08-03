package com.dmitriim.localaiplayground.core.audio.output.storage

import com.dmitriim.localaiplayground.core.audio.output.model.GeneratedAudioFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Reads and writes the fixed mono PCM16 WAV format retained by the TTS workflow. */
internal object WaveFileCodec {
    fun write(file: File, samples: FloatArray, sampleRateHz: Int) {
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

    fun readMetadata(file: File): GeneratedAudioFile? = runCatching {
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
            require(
                sampleRate > 0 &&
                    dataBytes >= 0 &&
                    dataBytes % PCM_BYTES_PER_SAMPLE == 0 &&
                    dataBytes.toLong() + WAV_HEADER_BYTES <= file.length(),
            )
            GeneratedAudioFile(
                filePath = file.absolutePath,
                sampleRateHz = sampleRate,
                sampleCount = dataBytes / PCM_BYTES_PER_SAMPLE,
                createdAtEpochMs = file.lastModified(),
            )
        }
    }.getOrNull()

    fun streamPcm16(audio: GeneratedAudioFile, onChunk: (ByteArray) -> Boolean) {
        val source = File(audio.filePath)
        val actual = requireNotNull(readMetadata(source)) { "The retained WAV is invalid." }
        requireMatches(expected = audio, actual = actual)
        BufferedInputStream(FileInputStream(source)).use { input ->
            skipHeader(input)
            var remaining = actual.sampleCount.toLong() * PCM_BYTES_PER_SAMPLE
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

    fun requireMatches(expected: GeneratedAudioFile, actual: GeneratedAudioFile) {
        require(
            expected.sampleRateHz == actual.sampleRateHz &&
                expected.sampleCount == actual.sampleCount &&
                expected.createdAtEpochMs == actual.createdAtEpochMs,
        ) {
            "The retained generated audio has been replaced."
        }
    }

    private fun skipHeader(input: BufferedInputStream) {
        var skipped = 0L
        while (skipped < WAV_HEADER_BYTES) {
            val count = input.skip(WAV_HEADER_BYTES - skipped)
            require(count > 0) { "The generated WAV header is truncated." }
            skipped += count
        }
    }

    private fun readAscii(input: RandomAccessFile, count: Int): String {
        val bytes = ByteArray(count)
        input.readFully(bytes)
        return bytes.toString(Charsets.US_ASCII)
    }

    const val PCM_BYTES_PER_SAMPLE = 2
    const val WAV_HEADER_BYTES = 44L
    private const val STREAM_CHUNK_BYTES = 32 * 1024
}
