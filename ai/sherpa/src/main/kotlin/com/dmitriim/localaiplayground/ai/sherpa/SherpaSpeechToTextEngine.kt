package com.dmitriim.localaiplayground.ai.sherpa

import com.dmitriim.localaiplayground.ai.api.SpeechToTextEngine
import com.dmitriim.localaiplayground.ai.api.SpeechToTextLoadRequest
import com.dmitriim.localaiplayground.ai.api.SpeechToTextLoadResult
import com.dmitriim.localaiplayground.ai.api.SpeechToTextRequest
import com.dmitriim.localaiplayground.ai.api.SpeechToTextResult
import com.dmitriim.localaiplayground.core.di.AppScope
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import kotlin.system.measureTimeMillis

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SherpaSpeechToTextEngine : SpeechToTextEngine {
    private val lock = Any()
    private var recognizer: OfflineRecognizer? = null
    @Volatile private var cancelled = false
    private var loadedDirectory: String? = null

    override val isLoaded: Boolean get() = synchronized(lock) { recognizer != null }

    override fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult = synchronized(lock) {
        val requestedDirectory = File(request.modelDirectory).canonicalPath
        if (recognizer != null && loadedDirectory == requestedDirectory) {
            return SpeechToTextLoadResult(effectiveThreadCount = effectiveThreads(request.threadCount), loadDurationMs = 0, coldStart = false)
        }
        unloadLocked()
        val required = SherpaProfiles.missingFiles(File(requestedDirectory), SherpaProfiles.whisperBaseRequiredFiles)
        require(required.isEmpty()) { "Whisper model files are missing: ${required.joinToString()}" }
        val threads = effectiveThreads(request.threadCount)
        val duration = measureTimeMillis {
            recognizer = OfflineRecognizer(null, OfflineRecognizerConfig().apply {
                modelConfig = OfflineModelConfig().apply {
                    whisper = OfflineWhisperModelConfig().apply {
                        encoder = File(requestedDirectory, "base-encoder.int8.onnx").absolutePath
                        decoder = File(requestedDirectory, "base-decoder.int8.onnx").absolutePath
                        language = request.languageCode
                        task = "transcribe"
                        enableSegmentTimestamps = true
                    }
                    tokens = File(requestedDirectory, "base-tokens.txt").absolutePath
                    numThreads = threads
                    provider = "cpu"
                    debug = false
                }
            })
        }
        loadedDirectory = requestedDirectory
        cancelled = false
        SpeechToTextLoadResult(threads, duration, coldStart = true)
    }

    override fun transcribe(request: SpeechToTextRequest): SpeechToTextResult = synchronized(lock) {
        check(!cancelled) { "Transcription was cancelled." }
        val activeRecognizer = checkNotNull(recognizer) { "Load a speech model before transcription." }
        require(request.samples.isNotEmpty()) { "The audio input is empty." }
        val stream = activeRecognizer.createStream()
        try {
            var text = ""
            val duration = measureTimeMillis {
                check(!cancelled) { "Transcription was cancelled." }
                stream.acceptWaveform(request.samples, request.sampleRateHz)
                activeRecognizer.decode(stream)
                check(!cancelled) { "Transcription was cancelled." }
                text = activeRecognizer.getResult(stream).text.trim()
            }
            SpeechToTextResult(text, duration)
        } finally {
            stream.release()
        }
    }

    override fun cancel() { cancelled = true }

    override fun unload() = synchronized(lock) { unloadLocked() }

    private fun unloadLocked() {
        recognizer?.release()
        recognizer = null
        loadedDirectory = null
        cancelled = false
    }

    private fun effectiveThreads(requested: Int): Int = requested.takeIf { it > 0 }
        ?: Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
}
