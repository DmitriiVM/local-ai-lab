package com.dmitriim.localaiplayground.feature.tts.domain

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.TextToSpeechEngine
import com.dmitriim.localaiplayground.ai.api.TextToSpeechLoadRequest
import com.dmitriim.localaiplayground.ai.api.TextToSpeechRequest
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffectsProcessor
import com.dmitriim.localaiplayground.core.model.LocalModelResolver
import com.dmitriim.localaiplayground.core.model.ModelId
import dev.zacsweers.metro.Inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException

data class SpeechPreviewRequest(
    val modelId: ModelId,
    val text: String,
    val voiceName: String,
    val settings: SpeechSynthesisSettings,
)

/** Synthesizes an ephemeral sample without retaining audio or creating run history. */
@Inject
class PreviewSpeech(
    private val modelResolver: LocalModelResolver,
    private val textToSpeechEngine: TextToSpeechEngine,
    private val player: StreamingSpeechPlayer,
    private val audioEffectsProcessor: SpeechAudioEffectsProcessor,
) {
    private val cancelled = AtomicBoolean(false)

    suspend fun execute(request: SpeechPreviewRequest) {
        request.settings.validate()
        require(request.text.isNotBlank()) { "The voice preview sample is empty." }
        cancelled.set(false)
        var playbackOpen = false
        var completed = false
        try {
            val model = modelResolver.resolveTextToSpeechModel(request.modelId).getOrThrow()
            val load = textToSpeechEngine.load(
                TextToSpeechLoadRequest(
                    profileType = model.profileType,
                    modelDirectory = model.modelDirectory,
                    threadCount = request.settings.threadCount,
                ),
            )
            require(
                request.settings.expectedSpeakerCount?.let { it == load.speakerCount } != false &&
                    request.settings.speakerId < load.speakerCount,
            ) {
                "${request.voiceName} is unavailable in the installed ${model.displayName} bundle."
            }
            checkNotCancelled()
            val effectsEnabled = !request.settings.audioEffects.isNeutral
            var session = if (effectsEnabled) {
                null
            } else {
                player.open(
                    sampleRateHz = load.sampleRateHz,
                    volume = request.settings.volume,
                    runAnchorNanos = System.nanoTime(),
                ).also { playbackOpen = true }
            }
            val streamingSession = session
            val result = textToSpeechEngine.synthesize(
                TextToSpeechRequest(
                    text = request.text,
                    languageCode = request.settings.languageCode,
                    speakerId = request.settings.speakerId,
                    speed = request.settings.speed,
                    sentenceSilenceScale = request.settings.sentenceSilenceScale,
                ),
            ) { chunk ->
                !cancelled.get() && (streamingSession == null || streamingSession.write(chunk))
            }
            checkNotCancelled()
            if (effectsEnabled) {
                val processed = audioEffectsProcessor.process(
                    samples = result.samples,
                    sampleRateHz = result.sampleRateHz,
                    effects = request.settings.audioEffects,
                    isCancelled = cancelled::get,
                )
                session = player.open(
                    sampleRateHz = load.sampleRateHz,
                    volume = request.settings.volume,
                    runAnchorNanos = System.nanoTime(),
                )
                playbackOpen = true
                writeFloatChunks(requireNotNull(session), processed)
            }
            requireNotNull(session).awaitDrained()
            checkNotCancelled()
            player.release(completed = true)
            playbackOpen = false
            completed = true
            Log.i(TAG, "Voice preview completed: model=${model.displayName}, voice=${request.voiceName}")
        } catch (error: Throwable) {
            if (cancelled.get()) {
                throw CancellationException("Voice preview stopped.").also { it.initCause(error) }
            }
            throw error
        } finally {
            try {
                runCatching { textToSpeechEngine.unload() }
            } finally {
                if (playbackOpen) player.release(completed = completed)
            }
        }
    }

    fun cancel() {
        cancelled.set(true)
        textToSpeechEngine.cancel()
        player.stop()
    }

    private fun checkNotCancelled() {
        if (cancelled.get()) throw CancellationException("Voice preview stopped.")
    }

    private fun writeFloatChunks(
        session: com.dmitriim.localaiplayground.core.audio.output.api.SpeechPlaybackSession,
        samples: FloatArray,
    ) {
        var offset = 0
        while (offset < samples.size) {
            checkNotCancelled()
            val end = minOf(offset + PROCESSED_AUDIO_CHUNK_SAMPLES, samples.size)
            if (!session.write(samples.copyOfRange(offset, end))) {
                throw CancellationException("Voice preview playback stopped.")
            }
            offset = end
        }
    }

    private companion object {
        const val TAG = "AiP123Tts"
        const val PROCESSED_AUDIO_CHUNK_SAMPLES = 4_096
    }
}
