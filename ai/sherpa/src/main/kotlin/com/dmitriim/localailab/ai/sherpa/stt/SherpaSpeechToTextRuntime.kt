package com.dmitriim.localailab.ai.sherpa.stt

import android.util.Log
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadResult
import com.dmitriim.localailab.ai.api.stt.SpeechToTextRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextResult
import com.dmitriim.localailab.ai.api.stt.SpeechToTextRuntime
import com.dmitriim.localailab.ai.runtime.model.ModelRuntimeProfileRegistry
import com.dmitriim.localailab.ai.sherpa.stt.profiles.SherpaSttProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.io.File
import kotlin.system.measureTimeMillis

@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<SpeechToTextRuntime>())
class SherpaSpeechToTextRuntime(
    private val profiles: ModelRuntimeProfileRegistry,
) : SpeechToTextRuntime {
    override val engineId = EngineId("sherpa-onnx")

    private val lock = Any()
    private var session: SherpaSttSession? = null

    @Volatile private var cancelled = false
    private var loadedModelKey: String? = null
    private var loadedThreads = 0

    override val isLoaded: Boolean
        get() = synchronized(lock) { session != null }

    override fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult = synchronized(lock) {
        require(request.engineId == engineId) { "Unsupported STT engine: ${request.engineId.value}" }
        val profile = profiles.requireTyped<SherpaSttProfile>(
            ModelProfileKey(request.engineId, request.profileType),
        )
        val artifacts = request.artifacts
        require(artifacts.isNotEmpty()) { "The speech model does not declare any artifacts." }
        val missing = artifacts.filterNot { artifact ->
            val file = File(artifact.path)
            if (artifact.directory) file.isDirectory else file.isFile
        }
        require(missing.isEmpty()) {
            "Speech model artifacts are missing: ${missing.joinToString { it.relativePath }}"
        }
        val threads = effectiveThreads(request.threadCount)
        val requestedModelKey = buildString {
            append(request.profileType.value)
            append('|').append(request.languageCode)
            artifacts.sortedWith(compareBy({ it.role.value }, { it.relativePath })).forEach { artifact ->
                append('|').append(artifact.role.value).append('=').append(artifact.path)
            }
        }
        if (isLoaded && loadedModelKey == requestedModelKey && loadedThreads == threads) {
            return SpeechToTextLoadResult(threads, loadDurationMs = 0, coldStart = false)
        }

        unloadLocked()
        Log.i(
            TAG,
            "Sherpa STT load requested: profile=${request.profileType.value}, " +
                "language=${request.languageCode}, threads=$threads",
        )
        val duration = try {
            measureTimeMillis {
                session = profile.open(request, ModelArtifacts(artifacts), threads)
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Sherpa STT native model creation failed: ${error.message}", error)
            unloadLocked()
            throw error
        }
        loadedModelKey = requestedModelKey
        loadedThreads = threads
        cancelled = false
        Log.i(TAG, "Sherpa STT model loaded: loadMs=$duration, effectiveThreads=$threads")
        SpeechToTextLoadResult(threads, duration, coldStart = true)
    }

    override fun transcribe(request: SpeechToTextRequest): SpeechToTextResult = synchronized(lock) {
        check(!cancelled) { "Transcription was cancelled." }
        require(request.samples.isNotEmpty()) { "The audio input is empty." }
        val active = checkNotNull(session) { "Load a speech model before transcription." }
        var text = ""
        val duration = measureTimeMillis {
            text = active.transcribe(request.samples, request.sampleRateHz) { cancelled }
        }
        Log.i(TAG, "Sherpa STT inference completed: processingMs=$duration, transcriptLength=${text.length}")
        SpeechToTextResult(text.trim(), duration)
    }

    override fun cancel() {
        cancelled = true
    }

    override fun unload() = synchronized(lock) { unloadLocked() }

    private fun unloadLocked() {
        session?.release()
        session = null
        loadedModelKey = null
        loadedThreads = 0
        cancelled = false
    }

    private fun effectiveThreads(requested: Int): Int = requested.takeIf { it > 0 }
        ?: Runtime.getRuntime().availableProcessors().coerceIn(1, 4)

    private companion object {
        const val TAG = "AiP123Stt"
    }
}
