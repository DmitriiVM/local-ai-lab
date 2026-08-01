package com.dmitriim.localaiplayground.core.audio.input.storage

import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.FileInputStream

/** Reads PCM16 without retaining an entire long recording in JVM memory. */
internal object PcmSegmentReader {
    suspend fun forEachSegment(input: PcmAudioInput, onSegment: (FloatArray) -> Unit) = withContext(Dispatchers.IO) {
        val bytes = ByteArray(input.sampleRateHz * MAX_SEGMENT_SECONDS * PCM16_BYTES)
        FileInputStream(input.file).use { stream ->
            while (true) {
                coroutineContext.ensureActive()
                var offset = 0
                while (offset < bytes.size) {
                    val read = stream.read(bytes, offset, bytes.size - offset)
                    if (read < 0) break
                    offset += read
                }
                if (offset == 0) break
                val floats = FloatArray(offset / PCM16_BYTES)
                for (index in floats.indices) {
                    val low = bytes[index * PCM16_BYTES].toInt() and 0xff
                    val high = bytes[index * PCM16_BYTES + 1].toInt()
                    floats[index] = ((high shl 8) or low).toShort() / 32768f
                }
                onSegment(floats)
                if (offset < bytes.size) break
            }
        }
    }

    private const val MAX_SEGMENT_SECONDS = 30
    private const val PCM16_BYTES = 2
}
