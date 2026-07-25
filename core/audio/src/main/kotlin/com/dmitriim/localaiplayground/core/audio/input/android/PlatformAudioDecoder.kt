package com.dmitriim.localaiplayground.core.audio.input.android

import android.app.Application
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class DecodedAudio(val frames: Long, val mimeType: String)

/** Decodes a document with platform codecs, downmixes it, then resamples it to PCM16. */
internal class PlatformAudioDecoder(private val application: Application) {
    fun decodeToMonoPcm(uri: Uri, output: File, targetRateHz: Int): DecodedAudio {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(application, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("The selected file has no supported audio track.")
            val format = extractor.getTrackFormat(track)
            val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME))
            extractor.selectTrack(track)
            codec = MediaCodec.createDecoderByType(mime).also { it.configure(format, null, null, 0); it.start() }
            var inputEnded = false
            var outputEnded = false
            val info = MediaCodec.BufferInfo()
            var writer: PcmResamplingWriter? = null
            BufferedOutputStream(FileOutputStream(output)).use { stream ->
                while (!outputEnded) {
                    if (!inputEnded) {
                        val index = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                        if (index >= 0) {
                            val buffer = requireNotNull(codec.getInputBuffer(index))
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    when (val index = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> writer = PcmResamplingWriter(stream, codec.outputFormat, targetRateHz)
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        else -> if (index >= 0) {
                            if (info.size > 0) {
                                val activeWriter = requireNotNull(writer) { "Audio decoder produced samples before its output format." }
                                val buffer = requireNotNull(codec.getOutputBuffer(index)).duplicate().apply {
                                    position(info.offset)
                                    limit(info.offset + info.size)
                                }
                                activeWriter.write(buffer)
                            }
                            codec.releaseOutputBuffer(index, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputEnded = true
                        }
                    }
                }
                return DecodedAudio(requireNotNull(writer) { "No decoded PCM was produced." }.writtenFrames, mime)
            }
        } finally {
            runCatching { codec?.stop() }
            codec?.release()
            extractor.release()
        }
    }

    private companion object { const val CODEC_TIMEOUT_US = 10_000L }
}

private class PcmResamplingWriter(
    private val stream: BufferedOutputStream,
    format: MediaFormat,
    private val targetRateHz: Int,
) {
    private val sourceRateHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
    private val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
    private val encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
        format.getInteger(MediaFormat.KEY_PCM_ENCODING)
    } else AudioFormat.ENCODING_PCM_16BIT
    private var sourceFrame = 0L
    private var nextOutputAt = 0.0
    var writtenFrames = 0L
        private set

    init {
        require(sourceRateHz > 0 && channels in 1..8) { "The decoded audio format is unsupported." }
    }

    fun write(buffer: ByteBuffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val bytesPerSample = when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            AudioFormat.ENCODING_PCM_16BIT -> 2
            else -> error("The selected audio uses an unsupported PCM encoding.")
        }
        val frameBytes = bytesPerSample * channels
        while (buffer.remaining() >= frameBytes) {
            var total = 0f
            repeat(channels) {
                total += when (encoding) {
                    AudioFormat.ENCODING_PCM_FLOAT -> buffer.float
                    else -> buffer.short / 32768f
                }
            }
            if (sourceFrame >= nextOutputAt) {
                val pcm = ((total / channels).coerceIn(-1f, 1f) * 32767f).toInt().toShort().toInt()
                stream.write(pcm and 0xff)
                stream.write((pcm ushr 8) and 0xff)
                writtenFrames++
                nextOutputAt += sourceRateHz.toDouble() / targetRateHz
            }
            sourceFrame++
        }
    }
}
