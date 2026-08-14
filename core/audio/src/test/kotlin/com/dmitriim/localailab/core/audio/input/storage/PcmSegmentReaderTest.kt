package com.dmitriim.localailab.core.audio.input.storage

import com.dmitriim.localailab.core.audio.input.model.PcmAudioInput
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PcmSegmentReaderTest {
    @Test
    fun decodesSignedLittleEndianPcm16Samples() = withPcmFile(
        byteArrayOf(
            0x00,
            0x80.toByte(),
            0xff.toByte(),
            0xff.toByte(),
            0x00,
            0x00,
            0xff.toByte(),
            0x7f,
        ),
    ) { input ->
        val segments = mutableListOf<FloatArray>()

        runBlocking {
            PcmSegmentReader.forEachSegment(input) { segment -> segments += segment }
        }

        assertEquals(1, segments.size)
        assertArrayEquals(floatArrayOf(-1f, -1f / 32768f, 0f, 32767f / 32768f), segments.single(), 0f)
    }

    @Test
    fun checksForCancellationBetweenPcmSegments() = withPcmFile(ByteArray(62)) { input ->
        assertThrows(CancellationException::class.java) {
            runBlocking {
                PcmSegmentReader.forEachSegment(input) {
                    coroutineContext.cancel()
                }
            }
        }
    }

    private inline fun withPcmFile(bytes: ByteArray, block: (PcmAudioInput) -> Unit) {
        val file = Files.createTempFile("pcm-segment", ".pcm").toFile()
        try {
            file.writeBytes(bytes)
            block(
                PcmAudioInput(
                    file = file,
                    displayName = "test.pcm",
                    durationMs = bytes.size / 2L * 1_000L,
                    sampleRateHz = 1,
                    sourceDescription = "Test",
                ),
            )
        } finally {
            file.delete()
        }
    }
}
