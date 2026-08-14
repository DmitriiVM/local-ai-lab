package com.dmitriim.localailab.ai.sherpa

import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import java.io.File
import java.io.RandomAccessFile
import kotlin.system.measureTimeMillis

/**
 * Direct, CPU-only sherpa-onnx spike. This is deliberately not a production
 * microphone or playback implementation; it proves model loading, inference,
 * file roles, and explicit native cleanup on a physical Android device.
 */
object StageZeroSherpa {
    fun transcribeWhisper(modelDirectory: File, wavFile: File): StageZeroSttResult {
        requireComplete(modelDirectory, SherpaProfiles.whisperBaseRequiredFiles, "Whisper Base")
        val audio = PcmWave.read(wavFile)
        val whisper = OfflineWhisperModelConfig().apply {
            encoder = File(modelDirectory, "base-encoder.int8.onnx").absolutePath
            decoder = File(modelDirectory, "base-decoder.int8.onnx").absolutePath
            language = "en"
            task = "transcribe"
        }
        val config = OfflineRecognizerConfig().apply {
            modelConfig = OfflineModelConfig().apply {
                this.whisper = whisper
                tokens = File(modelDirectory, "base-tokens.txt").absolutePath
                numThreads = 4
                provider = "cpu"
                debug = false
            }
        }
        val recognizer = OfflineRecognizer(null, config)
        val stream = recognizer.createStream()
        try {
            var text = ""
            val duration = measureTimeMillis {
                stream.acceptWaveform(audio.samples, audio.sampleRate)
                recognizer.decode(stream)
                text = recognizer.getResult(stream).text
            }
            return StageZeroSttResult(text = text, durationMs = duration)
        } finally {
            stream.release()
            recognizer.release()
        }
    }

    fun synthesizeSupertonic(modelDirectory: File, text: String): StageZeroTtsResult {
        requireComplete(modelDirectory, SherpaProfiles.supertonic3RequiredFiles, "Supertonic 3")
        val supertonic = OfflineTtsSupertonicModelConfig().apply {
            durationPredictor = File(modelDirectory, "duration_predictor.int8.onnx").absolutePath
            textEncoder = File(modelDirectory, "text_encoder.int8.onnx").absolutePath
            vectorEstimator = File(modelDirectory, "vector_estimator.int8.onnx").absolutePath
            vocoder = File(modelDirectory, "vocoder.int8.onnx").absolutePath
            ttsJson = File(modelDirectory, "tts.json").absolutePath
            unicodeIndexer = File(modelDirectory, "unicode_indexer.bin").absolutePath
            voiceStyle = File(modelDirectory, "voice.bin").absolutePath
        }
        val config = OfflineTtsConfig().apply {
            model = OfflineTtsModelConfig().apply {
                this.supertonic = supertonic
                numThreads = 4
                provider = "cpu"
                debug = false
            }
        }
        val tts = OfflineTts(null, config)
        try {
            lateinit var result: StageZeroTtsResult
            val duration = measureTimeMillis {
                val audio = tts.generate(text, 0, 1.0f)
                require(audio.samples.isNotEmpty()) { "Supertonic returned no PCM samples" }
                result = StageZeroTtsResult(
                    sampleRate = audio.sampleRate,
                    sampleCount = audio.samples.size,
                    durationMs = 0,
                )
            }
            return result.copy(durationMs = duration)
        } finally {
            tts.release()
        }
    }

    private fun requireComplete(directory: File, required: Set<String>, profile: String) {
        val missing = SherpaProfiles.missingFiles(directory, required)
        require(missing.isEmpty()) { "$profile files are missing: ${missing.joinToString()}" }
    }
    private data class PcmWave(val sampleRate: Int, val samples: FloatArray) {
        companion object {
            fun read(file: File): PcmWave = RandomAccessFile(file, "r").use { input ->
                require(readAscii(input, 4) == "RIFF") { "Not a RIFF WAV: ${file.name}" }
                input.skipBytes(4)
                require(readAscii(input, 4) == "WAVE") { "Not a WAVE file: ${file.name}" }
                var sampleRate = 0
                var channels = 0
                var bitsPerSample = 0
                var pcmData: ByteArray? = null
                while (input.filePointer + 8 <= input.length()) {
                    val chunk = readAscii(input, 4)
                    val size = Integer.reverseBytes(input.readInt())
                    require(size >= 0 && input.filePointer + size <= input.length()) {
                        "Invalid WAV chunk"
                    }
                    when (chunk) {
                        "fmt " -> {
                            require(size >= 16) { "Invalid WAV format chunk" }
                            val format = java.lang.Short.reverseBytes(input.readShort()).toInt()
                            channels = java.lang.Short.reverseBytes(input.readShort()).toInt()
                            sampleRate = Integer.reverseBytes(input.readInt())
                            input.skipBytes(6)
                            bitsPerSample = java.lang.Short.reverseBytes(input.readShort()).toInt()
                            require(format == 1 && channels == 1 && bitsPerSample == 16) {
                                "Only mono PCM16 WAV is supported by this Stage 0 probe"
                            }
                            input.seek(input.filePointer + size - 16)
                        }
                        "data" -> {
                            pcmData = ByteArray(size)
                            input.readFully(pcmData)
                        }
                        else -> input.seek(input.filePointer + size)
                    }
                    if (size % 2 == 1 && input.filePointer < input.length()) input.skipBytes(1)
                }
                val data = requireNotNull(pcmData) { "WAV data chunk is missing" }
                require(sampleRate > 0 && channels == 1 && bitsPerSample == 16) {
                    "WAV format chunk is missing"
                }
                val samples = FloatArray(data.size / 2)
                for (index in samples.indices) {
                    val lo = data[index * 2].toInt() and 0xff
                    val hi = data[index * 2 + 1].toInt()
                    samples[index] = ((hi shl 8) or lo).toShort() / 32768f
                }
                PcmWave(sampleRate, samples)
            }

            private fun readAscii(input: RandomAccessFile, size: Int): String {
                val bytes = ByteArray(size)
                input.readFully(bytes)
                return bytes.toString(Charsets.US_ASCII)
            }
        }
    }
}
