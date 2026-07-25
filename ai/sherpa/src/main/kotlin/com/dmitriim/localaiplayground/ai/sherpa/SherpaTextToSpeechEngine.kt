package com.dmitriim.localaiplayground.ai.sherpa

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.TextToSpeechEngine
import com.dmitriim.localaiplayground.ai.api.TextToSpeechLoadRequest
import com.dmitriim.localaiplayground.ai.api.TextToSpeechLoadResult
import com.dmitriim.localaiplayground.ai.api.TextToSpeechRequest
import com.dmitriim.localaiplayground.ai.api.TextToSpeechResult
import com.dmitriim.localaiplayground.core.di.AppScope
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SherpaTextToSpeechEngine : TextToSpeechEngine {
    private val lock = Any()
    private val cancelled = AtomicBoolean(false)
    private var tts: OfflineTts? = null
    private var loadedDirectory: String? = null
    private var loadedThreadCount: Int = 0

    override val isLoaded: Boolean get() = synchronized(lock) { tts != null }

    override fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult = synchronized(lock) {
        val requestedDirectory = File(request.modelDirectory).canonicalPath
        val threads = effectiveThreads(request.threadCount)
        Log.i(TAG, "Sherpa TTS load requested: directory=$requestedDirectory, requestedThreads=${request.threadCount}, effectiveThreads=$threads")
        val active = tts
        if (active != null && loadedDirectory == requestedDirectory && loadedThreadCount == threads) {
            Log.i(TAG, "Sherpa TTS model reuse: sampleRateHz=${active.sampleRate()}, speakers=${active.numSpeakers().coerceAtLeast(1)}")
            return TextToSpeechLoadResult(
                effectiveThreadCount = threads,
                loadDurationMs = 0,
                coldStart = false,
                sampleRateHz = active.sampleRate(),
                speakerCount = active.numSpeakers().coerceAtLeast(1),
            )
        }
        unloadLocked()
        val required = SherpaProfiles.missingFiles(
            File(requestedDirectory),
            SherpaProfiles.supertonic3RequiredFiles,
        )
        require(required.isEmpty()) {
            "Supertonic model files are missing: ${required.joinToString()}"
        }
        val config = OfflineTtsConfig().apply {
            model = OfflineTtsModelConfig().apply {
                supertonic = OfflineTtsSupertonicModelConfig().apply {
                    durationPredictor = File(requestedDirectory, "duration_predictor.int8.onnx").absolutePath
                    textEncoder = File(requestedDirectory, "text_encoder.int8.onnx").absolutePath
                    vectorEstimator = File(requestedDirectory, "vector_estimator.int8.onnx").absolutePath
                    vocoder = File(requestedDirectory, "vocoder.int8.onnx").absolutePath
                    ttsJson = File(requestedDirectory, "tts.json").absolutePath
                    unicodeIndexer = File(requestedDirectory, "unicode_indexer.bin").absolutePath
                    voiceStyle = File(requestedDirectory, "voice.bin").absolutePath
                }
                numThreads = threads
                provider = "cpu"
                debug = false
            }
        }
        var created: OfflineTts? = null
        val duration = try {
            measureTimeMillis {
                created = OfflineTts(null, config)
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Sherpa TTS native model creation failed: ${error.message}", error)
            throw error
        }
        val loaded = checkNotNull(created)
        tts = loaded
        loadedDirectory = requestedDirectory
        loadedThreadCount = threads
        cancelled.set(false)
        Log.i(
            TAG,
            "Sherpa TTS model loaded: loadMs=$duration, sampleRateHz=${loaded.sampleRate()}, " +
                "speakers=${loaded.numSpeakers().coerceAtLeast(1)}, threads=$threads",
        )
        TextToSpeechLoadResult(
            effectiveThreadCount = threads,
            loadDurationMs = duration,
            coldStart = true,
            sampleRateHz = loaded.sampleRate(),
            speakerCount = loaded.numSpeakers().coerceAtLeast(1),
        )
    }

    override fun synthesize(
        request: TextToSpeechRequest,
        onAudioChunk: (FloatArray) -> Boolean,
    ): TextToSpeechResult {
        val active = synchronized(lock) {
            checkNotNull(tts) { "Load a voice model before synthesis." }
        }
        require(request.text.isNotBlank()) { "Enter text to synthesize." }
        require(request.speed in 0.5f..2f) { "Speech rate must be between 0.5 and 2.0." }
        require(request.sentenceSilenceScale in 0f..2f) {
            "Sentence silence must be between 0.0 and 2.0."
        }
        cancelled.set(false)
        Log.i(
            TAG,
            "Sherpa TTS synthesis started: textLength=${request.text.length}, language=${request.languageCode}, " +
                "speaker=${request.speakerId}, speed=${request.speed}, silenceScale=${request.sentenceSilenceScale}",
        )
        val generationConfig = GenerationConfig().apply {
            speed = request.speed
            sid = request.speakerId
            silenceScale = request.sentenceSilenceScale
            extra = mapOf("lang" to request.languageCode)
        }
        val audio = try {
            active.generateWithConfigAndCallback(
                request.text,
                generationConfig,
                AudioChunkCallback(cancelled, onAudioChunk),
            )
        } catch (error: Throwable) {
            Log.e(TAG, "Sherpa TTS native synthesis failed: ${error.message}", error)
            throw error
        }
        check(!cancelled.get()) { "Speech synthesis was cancelled." }
        require(audio.samples.isNotEmpty()) { "The voice model returned no audio." }
        Log.i(TAG, "Sherpa TTS synthesis completed: samples=${audio.samples.size}, sampleRateHz=${audio.sampleRate}")
        return TextToSpeechResult(
            samples = audio.samples,
            sampleRateHz = audio.sampleRate,
        )
    }

    override fun cancel() {
        Log.i(TAG, "Sherpa TTS cancellation flag set.")
        cancelled.set(true)
    }

    override fun unload() = synchronized(lock) {
        Log.i(TAG, "Sherpa TTS unload requested.")
        unloadLocked()
    }

    private fun unloadLocked() {
        if (tts != null) Log.i(TAG, "Sherpa TTS releasing native model.")
        cancelled.set(true)
        tts?.release()
        tts = null
        loadedDirectory = null
        loadedThreadCount = 0
    }

    private fun effectiveThreads(requested: Int): Int = requested.takeIf { it > 0 }
        ?: Runtime.getRuntime().availableProcessors().coerceIn(1, 4)

    /**
     * The sherpa-onnx JNI binding looks up this exact specialised Kotlin method:
     * `invoke(float[]): Integer`. Do not replace this with an inline lambda: newer
     * Android desugaring emits only the erased `invoke(Object): Object` method.
     */
    private class AudioChunkCallback(
        private val cancelled: AtomicBoolean,
        private val onAudioChunk: (FloatArray) -> Boolean,
    ) : Function1<FloatArray, Int> {
        override fun invoke(samples: FloatArray): Int = when {
            cancelled.get() -> 0
            samples.isEmpty() || onAudioChunk(samples) -> 1
            else -> {
                cancelled.set(true)
                0
            }
        }
    }

    private companion object {
        const val TAG = "AiP123Tts"
    }
}
