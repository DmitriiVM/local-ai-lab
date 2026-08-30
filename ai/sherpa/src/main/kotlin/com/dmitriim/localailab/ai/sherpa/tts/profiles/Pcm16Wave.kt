package com.dmitriim.localailab.ai.sherpa.tts.profiles

import java.io.File
import java.io.RandomAccessFile
import java.lang.Short
import kotlin.ByteArray
import kotlin.FloatArray
import kotlin.Int
import kotlin.also
import kotlin.require
import kotlin.requireNotNull
import kotlin.text.Charsets

class Pcm16Wave(val sampleRateHz: Int, val samples: FloatArray) {
    companion object {
        fun read(file: File): Pcm16Wave = RandomAccessFile(file, "r").use { input ->
            require(readAscii(input, 4) == "RIFF") { "Reference audio is not a RIFF WAV." }
            input.skipBytes(4)
            require(readAscii(input, 4) == "WAVE") { "Reference audio is not a WAV file." }
            var sampleRateHz = 0
            var channels = 0
            var bitsPerSample = 0
            var pcmData: ByteArray? = null
            while (input.filePointer + 8 <= input.length()) {
                val chunk = readAscii(input, 4)
                val size = Integer.reverseBytes(input.readInt())
                require(size >= 0 && input.filePointer + size <= input.length()) {
                    "Reference WAV is invalid."
                }
                when (chunk) {
                    "fmt " -> {
                        require(size >= 16) { "Reference WAV format is invalid." }
                        val format = Short.reverseBytes(input.readShort()).toInt()
                        channels = Short.reverseBytes(input.readShort()).toInt()
                        sampleRateHz = Integer.reverseBytes(input.readInt())
                        input.skipBytes(6)
                        bitsPerSample = Short.reverseBytes(input.readShort()).toInt()
                        require(format == 1 && channels == 1 && bitsPerSample == 16) {
                            "Reference audio must be mono PCM16 WAV."
                        }
                        input.seek(input.filePointer + size - 16)
                    }
                    "data" -> pcmData = ByteArray(size).also(input::readFully)
                    else -> input.seek(input.filePointer + size)
                }
                if (size % 2 == 1 && input.filePointer < input.length()) input.skipBytes(1)
            }
            val data = requireNotNull(pcmData) { "Reference WAV data is missing." }
            require(sampleRateHz > 0 && channels == 1 && bitsPerSample == 16) {
                "Reference WAV format is missing."
            }
            Pcm16Wave(
                sampleRateHz,
                FloatArray(data.size / 2) { index ->
                    val low = data[index * 2].toInt() and 0xff
                    val high = data[index * 2 + 1].toInt()
                    ((high shl 8) or low).toShort() / 32_768f
                },
            )
        }

        private fun readAscii(input: RandomAccessFile, length: Int) = ByteArray(length).also(input::readFully).toString(Charsets.US_ASCII)
    }
}