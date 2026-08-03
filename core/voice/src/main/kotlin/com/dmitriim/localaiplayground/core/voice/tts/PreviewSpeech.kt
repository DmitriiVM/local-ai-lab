package com.dmitriim.localaiplayground.core.voice.tts

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechEngine
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechLoadRequest
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffectsProcessor
import com.dmitriim.localaiplayground.core.model.service.LocalModelResolver
import dev.zacsweers.metro.Inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException

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
                    engineId = model.engineId,
                    profileType = model.profileType,
                    modelDirectory = model.modelDirectory,
                    threadCount = request.settings.threadCount,
                ),
            )
            val fixedSpeaker = request.settings.voiceCondition as?
                com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechVoiceCondition.FixedSpeaker
            if (fixedSpeaker != null) {
                require(
                    request.settings.expectedSpeakerCount?.let { it == load.speakerCount } != false &&
                        load.speakerCount?.let { fixedSpeaker.speakerId < it } == true,
                ) {
                    "${request.voiceName} is unavailable in the installed ${model.displayName} bundle."
                }
            }
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
