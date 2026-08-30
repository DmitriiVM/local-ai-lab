package com.dmitriim.localailab.ai.sherpa.tts

import android.util.Log
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadResult
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechResult
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRuntime
import com.dmitriim.localailab.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localailab.ai.runtime.model.ModelRuntimeProfileRegistry
import com.dmitriim.localailab.ai.sherpa.tts.profiles.SherpaTtsProfile
import com.dmitriim.localailab.core.di.AppScope
import com.k2fsa.sherpa.onnx.GenerationConfig
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<TextToSpeechRuntime>())
class SherpaTextToSpeechRuntime(
    private val profiles: ModelRuntimeProfileRegistry,
) : TextToSpeechRuntime {
    override val engineId = EngineId("sherpa-onnx")
    private val lock = Any()
    private val cancelled = AtomicBoolean(false)
    private var model: SherpaTtsModel? = null
    private var loadedProfile: SherpaTtsProfile? = null
    private var loadedModelKey: String? = null
    private var loadedThreadCount = 0

    override val isLoaded: Boolean get() = synchronized(lock) { model != null }

    override fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult = synchronized(lock) {
        require(request.engineId == engineId) { "Unsupported TTS engine: ${request.engineId.value}" }
        val profile = profiles.requireTyped<SherpaTtsProfile>(
            ModelProfileKey(request.engineId, request.profileType),
        )
        val artifacts = request.artifacts
        require(artifacts.isNotEmpty()) { "The voice model does not declare any artifacts." }
        val missing = artifacts.filterNot { artifact ->
            val file = File(artifact.path)
            if (artifact.directory) file.isDirectory else file.isFile
        }
        require(missing.isEmpty()) {
            "Voice model artifacts are missing: ${missing.joinToString { it.relativePath }}"
        }
        val threads = effectiveThreads(request.threadCount)
        val requestedModelKey = buildString {
            append(request.profileType.value)
            artifacts.sortedWith(compareBy({ it.role.value }, { it.relativePath })).forEach { artifact ->
                append('|').append(artifact.role.value).append('=').append(artifact.path)
            }
        }
        val active = model
        if (active != null && loadedModelKey == requestedModelKey && loadedThreadCount == threads) {
            cancelled.set(false)
            return TextToSpeechLoadResult(
                effectiveThreadCount = threads,
                loadDurationMs = 0,
                coldStart = false,
                sampleRateHz = active.runtime.sampleRate(),
                speakerCount = active.runtime.numSpeakers().coerceAtLeast(1),
            )
        }

        unloadLocked()
        Log.i(TAG, "Sherpa TTS load requested: profile=${request.profileType.value}, threads=$threads")
        var created: SherpaTtsModel? = null
        val duration = try {
            measureTimeMillis { created = profile.open(ModelArtifacts(artifacts), threads) }
        } catch (error: Throwable) {
            Log.e(TAG, "Sherpa TTS native model creation failed: ${error.message}", error)
            throw error
        }
        val loaded = checkNotNull(created)
        model = loaded
        loadedProfile = profile
        loadedModelKey = requestedModelKey
        loadedThreadCount = threads
        cancelled.set(false)
        Log.i(
            TAG,
            "Sherpa TTS model loaded: loadMs=$duration, sampleRateHz=${loaded.runtime.sampleRate()}, " +
                "speakers=${loaded.runtime.numSpeakers().coerceAtLeast(1)}, threads=$threads",
        )
        TextToSpeechLoadResult(
            effectiveThreadCount = threads,
            loadDurationMs = duration,
            coldStart = true,
            sampleRateHz = loaded.runtime.sampleRate(),
            speakerCount = loaded.runtime.numSpeakers().coerceAtLeast(1),
        )
    }

    override fun synthesize(
        request: TextToSpeechRequest,
        onAudioChunk: (FloatArray) -> Boolean,
    ): TextToSpeechResult {
        val (active, profile) = synchronized(lock) {
            checkNotNull(model) { "Load a voice model before synthesis." } to checkNotNull(loadedProfile)
        }
        require(request.text.isNotBlank()) { "Enter text to synthesize." }
        require(request.speed in 0.5f..2f) { "Speech rate must be between 0.5 and 2.0." }
        require(request.sentenceSilenceScale in MIN_SENTENCE_SILENCE_SCALE..MAX_SENTENCE_SILENCE_SCALE) {
            "Sentence silence must be between $MIN_SENTENCE_SILENCE_SCALE and $MAX_SENTENCE_SILENCE_SCALE for Sherpa TTS."
        }
        val speaker = request.voice as? TextToSpeechVoiceCondition.FixedSpeaker
            ?: error("Sherpa TTS requires a fixed speaker voice.")
        cancelled.set(false)
        val generationConfig = GenerationConfig().apply {
            speed = request.speed
            sid = speaker.speakerId
            silenceScale = request.sentenceSilenceScale
            profile.configureGeneration(this, request, active)
        }
        val audio = try {
            active.runtime.generateWithConfigAndCallback(
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
        return TextToSpeechResult(audio.samples, audio.sampleRate)
    }

    override fun cancel() {
        cancelled.set(true)
    }

    override fun unload() = synchronized(lock) { unloadLocked() }

    private fun unloadLocked() {
        cancelled.set(true)
        model?.runtime?.release()
        model = null
        loadedProfile = null
        loadedModelKey = null
        loadedThreadCount = 0
    }

    private fun effectiveThreads(requested: Int): Int = requested.takeIf { it > 0 }
        ?: Runtime.getRuntime().availableProcessors().coerceIn(1, 4)

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
        const val MIN_SENTENCE_SILENCE_SCALE = 0.001f
        const val MAX_SENTENCE_SILENCE_SCALE = 1f
    }
}
