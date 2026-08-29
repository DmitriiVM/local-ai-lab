package com.dmitriim.localailab.feature.tts.impl.domain.synthesis

import android.util.Log
import com.dmitriim.localailab.ai.api.tts.TextToSpeechEngine
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localailab.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localailab.core.audio.processing.SpeechAudioEffectsProcessor
import com.dmitriim.localailab.feature.models.api.domain.runtime.LocalModelResolver
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.tts.api.domain.PreviewSpeech
import com.dmitriim.localailab.feature.tts.api.domain.SpeechPreviewRequest
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException

/** Synthesizes an ephemeral sample without retaining audio or creating run history. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<PreviewSpeech>())
class PreviewSpeechService(
    private val modelResolver: LocalModelResolver,
    private val textToSpeechEngine: TextToSpeechEngine,
    private val player: StreamingSpeechPlayer,
    private val audioEffectsProcessor: SpeechAudioEffectsProcessor,
) : PreviewSpeech {
    private val cancelled = AtomicBoolean(false)

    override suspend fun execute(request: SpeechPreviewRequest) {
        request.settings.validate()
        require(request.text.isNotBlank()) { "The voice preview sample is empty." }
        cancelled.set(false)
        var playbackOpen = false
        var completed = false
        try {
            val model = modelResolver.resolveTextToSpeechModel(request.modelId).getOrThrow()
            val load = textToSpeechEngine.load(
                TextToSpeechLoadRequest(
                    engineId = model.engineId,
                    profileType = model.profileType,
                    modelDirectory = model.modelDirectory,
                    threadCount = request.settings.threadCount,
                    artifacts = model.artifacts,
                ),
            )
            validateSpeaker(request, model.displayName, load.speakerCount)
            checkNotCancelled()
            val effectsEnabled = !request.settings.audioEffects.isNeutral
            val canStreamImmediately = !effectsEnabled && load.sampleRateHz > 0
            var session = if (!canStreamImmediately) {
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
                    voice = request.settings.voiceCondition,
                    speed = request.settings.speed,
                    sentenceSilenceScale = request.settings.sentenceSilenceScale,
                ),
            ) { chunk ->
                !cancelled.get() && (streamingSession == null || streamingSession.write(chunk))
            }
            checkNotCancelled()
            require(load.sampleRateHz == 0 || result.sampleRateHz == load.sampleRateHz) {
                "The voice model changed sample rate during synthesis."
            }
            if (session == null) {
                val output = if (effectsEnabled) {
                    audioEffectsProcessor.process(
                        samples = result.samples,
                        sampleRateHz = result.sampleRateHz,
                        effects = request.settings.audioEffects,
                        isCancelled = cancelled::get,
                    )
                } else {
                    result.samples
                }
                session = player.open(
                    sampleRateHz = result.sampleRateHz,
                    volume = request.settings.volume,
                    runAnchorNanos = System.nanoTime(),
                )
                playbackOpen = true
                writeFloatChunks(requireNotNull(session), output)
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
            if (playbackOpen) player.release(completed = completed)
        }
    }

    override fun cancel() {
        cancelled.set(true)
        textToSpeechEngine.cancel()
        player.stop()
    }

    private fun validateSpeaker(
        request: SpeechPreviewRequest,
        modelName: String,
        speakerCount: Int?,
    ) {
        val speaker = request.settings.voiceCondition as?
            com.dmitriim.localailab.ai.api.tts.TextToSpeechVoiceCondition.FixedSpeaker ?: return
        require(
            request.settings.expectedSpeakerCount?.let { it == speakerCount } != false &&
                speakerCount?.let { speaker.speakerId < it } == true,
        ) {
            "${request.voiceName} is unavailable in the installed $modelName bundle."
        }
    }

    private fun checkNotCancelled() {
        if (cancelled.get()) throw CancellationException("Voice preview stopped.")
    }

    private fun writeFloatChunks(
        session: com.dmitriim.localailab.core.audio.output.api.SpeechPlaybackSession,
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
