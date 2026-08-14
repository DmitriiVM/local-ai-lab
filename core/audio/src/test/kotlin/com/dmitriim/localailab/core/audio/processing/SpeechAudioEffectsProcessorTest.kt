package com.dmitriim.localailab.core.audio.processing

import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechAudioEffectsProcessorTest {
    private val processor = SpeechAudioEffectsProcessor()

    @Test
    fun saturationOutputIsFiniteAndClampedToPcmRange() {
        val output = processor.process(
            samples = floatArrayOf(-0.99f, -0.5f, 0f, 0.5f, 0.99f),
            sampleRateHz = 16_000,
            effects = SpeechAudioEffects(saturationDriveDb = 24f),
        )

        assertTrue(output.all { sample -> sample.isFinite() && abs(sample) <= 1f })
    }

    @Test
    fun cancellationStopsNonNeutralProcessing() {
        assertThrows(CancellationException::class.java) {
            processor.process(
                samples = floatArrayOf(0.5f),
                sampleRateHz = 16_000,
                effects = SpeechAudioEffects(saturationDriveDb = 1f),
                isCancelled = { true },
            )
        }
    }
}
