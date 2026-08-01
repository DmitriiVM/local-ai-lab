package com.dmitriim.localaiplayground.ai.vosk

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.stt.SpeechToTextBackend
import com.dmitriim.localaiplayground.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localaiplayground.ai.api.stt.SpeechToTextLoadResult
import com.dmitriim.localaiplayground.ai.api.stt.SpeechToTextRequest
import com.dmitriim.localaiplayground.ai.api.stt.SpeechToTextResult
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileIds
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<SpeechToTextBackend>())
class VoskSpeechToTextEngine : SpeechToTextBackend {
    override val engineId = EngineId("vosk")

    private val lock = Any()
    private var model: Model? = null
    private var modelPath: String? = null
    @Volatile private var cancelled = false

    override val isLoaded: Boolean
        get() = synchronized(lock) { model != null }

    override fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult = synchronized(lock) {
        require(request.engineId == engineId) { "Unsupported STT engine: ${request.engineId.value}" }
        require(request.profileType == ModelProfileIds.VOSK_STT) {
            "Unsupported Vosk profile: ${request.profileType.value}"
        }
        val directory = File(request.modelDirectory).canonicalFile
        require(directory.isDirectory) { "The Vosk model directory is missing." }
        if (model != null && modelPath == directory.path) {
            return SpeechToTextLoadResult(1, loadDurationMs = 0, coldStart = false)
        }
        unloadLocked()
        val duration = measureTimeMillis {
            model = Model(directory.path)
        }
        modelPath = directory.path
        cancelled = false
        Log.i(TAG, "Vosk model loaded: directory=${directory.name}, loadMs=$duration")
        SpeechToTextLoadResult(1, duration, coldStart = true)
    }

    override fun transcribe(request: SpeechToTextRequest): SpeechToTextResult = synchronized(lock) {
        check(!cancelled) { "Transcription was cancelled." }
        require(request.samples.isNotEmpty()) { "The audio input is empty." }
        val activeModel = checkNotNull(model) { "Load a Vosk model before transcription." }
        var text = ""
        val duration = measureTimeMillis {
            Recognizer(activeModel, request.sampleRateHz.toFloat()).use { recognizer ->
                val chunkSize = request.sampleRateHz.coerceAtLeast(1)
                val pcm = ShortArray(chunkSize)
                val transcript = StringBuilder()
                var offset = 0
                while (offset < request.samples.size) {
                    check(!cancelled) { "Transcription was cancelled." }
                    val size = minOf(chunkSize, request.samples.size - offset)
                    repeat(size) { index ->
                        pcm[index] = (request.samples[offset + index].coerceIn(-1f, 1f) * Short.MAX_VALUE)
                            .roundToInt()
                            .toShort()
                    }
                    if (recognizer.acceptWaveForm(pcm, size)) {
                        appendTranscript(transcript, recognizer.result)
                    }
                    offset += size
                }
                appendTranscript(transcript, recognizer.finalResult)
                text = transcript.toString()
            }
        }
        SpeechToTextResult(text.trim(), duration)
    }

    override fun cancel() {
        cancelled = true
    }

    override fun unload() = synchronized(lock) {
        unloadLocked()
    }

    private fun unloadLocked() {
        model?.close()
        model = null
        modelPath = null
        cancelled = false
    }

    private fun appendTranscript(transcript: StringBuilder, resultJson: String) {
        val result = Json.parseToJsonElement(resultJson)
            .jsonObject["text"]
            ?.jsonPrimitive
            ?.content
            .orEmpty()
            .trim()
        if (result.isEmpty()) return
        if (transcript.isNotEmpty()) transcript.append(' ')
        transcript.append(result)
    }

    private companion object {
        const val TAG = "AiP123Stt"
    }
}
