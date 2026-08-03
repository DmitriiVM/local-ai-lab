package com.dmitriim.localaiplayground.core.audio.output.storage

import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class WaveFileCodecTest {
    @Test
    fun writesReadableMonoPcm16WaveWithClampedSamples() = withWaveFile { file ->
        WaveFileCodec.write(file, floatArrayOf(-2f, 0f, 2f), sampleRateHz = 16_000)

        val metadata = requireNotNull(WaveFileCodec.readMetadata(file))
        val chunks = mutableListOf<ByteArray>()
        WaveFileCodec.streamPcm16(metadata) { chunk ->
            chunks += chunk
            true
        }

        assertEquals(16_000, metadata.sampleRateHz)
        assertEquals(3, metadata.sampleCount)
        assertArrayEquals(
            byteArrayOf(1, 0x80.toByte(), 0, 0, 0xff.toByte(), 0x7f),
            chunks.flatMap(ByteArray::asIterable).toByteArray(),
        )
    }

    @Test
    fun rejectsStaleGeneratedAudioMetadata() = withWaveFile { file ->
        WaveFileCodec.write(file, floatArrayOf(0f), sampleRateHz = 16_000)
        val actual = requireNotNull(WaveFileCodec.readMetadata(file))

        assertThrows(IllegalArgumentException::class.java) {
            WaveFileCodec.requireMatches(actual.copy(sampleCount = 2), actual)
        }
    }

    @Test
    fun rejectsWaveWithOddPcmDataLength() = withWaveFile { file ->
        WaveFileCodec.write(file, floatArrayOf(0f), sampleRateHz = 16_000)
        RandomAccessFile(file, "rw").use { output ->
            output.seek(40)
            output.writeInt(Integer.reverseBytes(1))
        }

        assertNull(WaveFileCodec.readMetadata(file))
    }

    private inline fun withWaveFile(block: (File) -> Unit) {
        val file = Files.createTempFile("generated-audio", ".wav").toFile()
        try {
            block(file)
        } finally {
            file.delete()
        }
    }
}
