package com.dmitriim.localaiplayground.core.audio.processing

import dev.zacsweers.metro.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlinx.coroutines.CancellationException

/**
 * Offline mono speech effects. Processing a complete synthesis keeps pitch and
 * formant analysis continuous and guarantees that playback and exported WAVs match.
 */
@Inject
class SpeechAudioEffectsProcessor {
    fun process(
        samples: FloatArray,
        sampleRateHz: Int,
        effects: SpeechAudioEffects,
        isCancelled: () -> Boolean = { false },
    ): FloatArray {
        require(samples.isNotEmpty()) { "Generated audio is empty." }
        require(sampleRateHz > 0) { "Generated audio has an invalid sample rate." }
        effects.validate()
        if (effects.isNeutral) return samples

        checkNotCancelled(isCancelled)
        var output = samples.copyOf()
        if (abs(effects.pitchSemitones) >= EFFECT_EPSILON) {
            output = shiftPitch(output, sampleRateHz, effects.pitchSemitones, isCancelled)
        }
        if (abs(effects.formantSemitones) >= EFFECT_EPSILON) {
            output = shiftFormants(output, sampleRateHz, effects.formantSemitones, isCancelled)
        }
        if (
            abs(effects.lowEqDb) >= EFFECT_EPSILON ||
            abs(effects.midEqDb) >= EFFECT_EPSILON ||
            abs(effects.highEqDb) >= EFFECT_EPSILON
        ) {
            applyEqualizer(
                samples = output,
                sampleRateHz = sampleRateHz,
                lowDb = effects.lowEqDb,
                midDb = effects.midEqDb,
                highDb = effects.highEqDb,
                isCancelled = isCancelled,
            )
        }
        if (effects.saturationDriveDb >= EFFECT_EPSILON) {
            applySaturation(output, effects.saturationDriveDb, isCancelled)
        }
        preventClipping(output)
        checkNotCancelled(isCancelled)
        return output
    }

    private fun shiftPitch(
        input: FloatArray,
        sampleRateHz: Int,
        semitones: Float,
        isCancelled: () -> Boolean,
    ): FloatArray {
        val factor = 2.0.pow(semitones / 12.0).toFloat()
        val stretchedSize = (input.size * factor).roundToInt().coerceAtLeast(1)
        val stretched = timeStretchWsola(input, sampleRateHz, stretchedSize, isCancelled)
        return resampleLinear(stretched, input.size, isCancelled)
    }

    /**
     * Waveform-similarity overlap-add keeps local speech periods coherent while
     * changing duration. Resampling the stretched result restores the original
     * duration and produces the requested pitch shift.
     */
    private fun timeStretchWsola(
        input: FloatArray,
        sampleRateHz: Int,
        targetSize: Int,
        isCancelled: () -> Boolean,
    ): FloatArray {
        if (input.size < MIN_PITCH_SAMPLES || targetSize == input.size) {
            return resampleLinear(input, targetSize, isCancelled)
        }
        val windowSize = (sampleRateHz * WSOLA_WINDOW_SECONDS)
            .roundToInt()
            .coerceIn(MIN_WSOLA_WINDOW, input.size.coerceAtMost(MAX_WSOLA_WINDOW))
            .let { it - it % 2 }
            .coerceAtLeast(2)
        if (input.size <= windowSize) return resampleLinear(input, targetSize, isCancelled)

        val overlap = windowSize / 2
        val synthesisHop = windowSize - overlap
        val stretch = targetSize.toDouble() / input.size
        val analysisHop = synthesisHop / stretch
        val searchRadius = (sampleRateHz * WSOLA_SEARCH_SECONDS)
            .roundToInt()
            .coerceAtLeast(1)
        val output = FloatArray(targetSize)
        val initialCount = min(windowSize, targetSize)
        input.copyInto(output, endIndex = initialCount)

        var outputPosition = synthesisHop
        var expectedInputPosition = analysisHop
        var iteration = 0
        while (outputPosition < targetSize) {
            if (iteration++ % CANCELLATION_CHECK_INTERVAL == 0) checkNotCancelled(isCancelled)
            val maximumCandidate = (input.size - windowSize).coerceAtLeast(0)
            val nominal = expectedInputPosition.roundToInt().coerceIn(0, maximumCandidate)
            val searchStart = (nominal - searchRadius).coerceAtLeast(0)
            val searchEnd = (nominal + searchRadius).coerceAtMost(maximumCandidate)
            val overlapCount = min(overlap, targetSize - outputPosition)
            val candidate = bestOverlapCandidate(
                output = output,
                outputPosition = outputPosition,
                input = input,
                searchStart = searchStart,
                searchEnd = searchEnd,
                overlapCount = overlapCount,
            )

            for (index in 0 until overlapCount) {
                val blend = index.toFloat() / overlap.coerceAtLeast(1)
                output[outputPosition + index] =
                    output[outputPosition + index] * (1f - blend) +
                    input[candidate + index] * blend
            }
            val frameCount = min(windowSize, targetSize - outputPosition)
            for (index in overlapCount until frameCount) {
                output[outputPosition + index] = input[candidate + index]
            }
            outputPosition += synthesisHop
            expectedInputPosition += analysisHop
        }
        return output
    }

    private fun bestOverlapCandidate(
        output: FloatArray,
        outputPosition: Int,
        input: FloatArray,
        searchStart: Int,
        searchEnd: Int,
        overlapCount: Int,
    ): Int {
        if (overlapCount <= 0 || searchStart >= searchEnd) return searchStart
        var bestPosition = searchStart
        var bestScore = Double.NEGATIVE_INFINITY
        var candidate = searchStart
        while (candidate <= searchEnd) {
            var cross = 0.0
            var outputEnergy = 0.0
            var inputEnergy = 0.0
            var index = 0
            while (index < overlapCount) {
                val left = output[outputPosition + index].toDouble()
                val right = input[candidate + index].toDouble()
                cross += left * right
                outputEnergy += left * left
                inputEnergy += right * right
                index += WSOLA_CORRELATION_STRIDE
            }
            val denominator = sqrt(outputEnergy * inputEnergy)
            val score = if (denominator > SILENCE_ENERGY) cross / denominator else 0.0
            if (score > bestScore) {
                bestScore = score
                bestPosition = candidate
            }
            candidate += WSOLA_SEARCH_STRIDE
        }
        return bestPosition
    }

    private fun resampleLinear(
        input: FloatArray,
        targetSize: Int,
        isCancelled: () -> Boolean,
    ): FloatArray {
        require(targetSize > 0)
        if (targetSize == input.size) return input.copyOf()
        if (input.size == 1) return FloatArray(targetSize) { input[0] }
        val output = FloatArray(targetSize)
        val scale = (input.size - 1).toDouble() / (targetSize - 1).coerceAtLeast(1)
        for (index in output.indices) {
            if (index % SAMPLE_CANCELLATION_INTERVAL == 0) checkNotCancelled(isCancelled)
            val source = index * scale
            val left = source.toInt().coerceAtMost(input.lastIndex)
            val right = (left + 1).coerceAtMost(input.lastIndex)
            val fraction = (source - left).toFloat()
            output[index] = input[left] + (input[right] - input[left]) * fraction
        }
        return output
    }

    /**
     * Moves the smooth spectral envelope while retaining the fine harmonic
     * structure at its original bins, which approximates independent formant shift.
     */
    private fun shiftFormants(
        input: FloatArray,
        sampleRateHz: Int,
        semitones: Float,
        isCancelled: () -> Boolean,
    ): FloatArray {
        val factor = 2.0.pow(semitones / 12.0)
        val frameSize = nextPowerOfTwo((sampleRateHz * FORMANT_FRAME_SECONDS).roundToInt())
            .coerceIn(MIN_FFT_SIZE, MAX_FFT_SIZE)
        val hopSize = frameSize / FORMANT_OVERLAP_DIVISOR
        val padding = frameSize / 2
        val paddedSize = input.size + frameSize
        val padded = FloatArray(paddedSize)
        input.copyInto(padded, destinationOffset = padding)
        val frameCount = ceil((paddedSize - frameSize).toDouble() / hopSize).toInt() + 1
        val synthesisSize = (frameCount - 1) * hopSize + frameSize
        val accumulated = FloatArray(synthesisSize)
        val weights = FloatArray(synthesisSize)
        val window = FloatArray(frameSize) { index ->
            sqrt(0.5f - 0.5f * cos((2.0 * PI * index / (frameSize - 1)).toFloat()))
        }
        val real = FloatArray(frameSize)
        val imaginary = FloatArray(frameSize)
        val binCount = frameSize / 2 + 1
        val logMagnitude = FloatArray(binCount)
        val envelope = FloatArray(binCount)
        val prefix = DoubleArray(binCount + 1)
        val binWidthHz = sampleRateHz.toFloat() / frameSize
        val smoothingRadius = (FORMANT_SMOOTHING_HZ / binWidthHz)
            .roundToInt()
            .coerceAtLeast(2)
        val maximumEnvelopeDelta = (MAX_FORMANT_GAIN_DB / 20.0 * ln(10.0)).toFloat()

        repeat(frameCount) { frameIndex ->
            if (frameIndex % CANCELLATION_CHECK_INTERVAL == 0) checkNotCancelled(isCancelled)
            val frameStart = frameIndex * hopSize
            real.fill(0f)
            imaginary.fill(0f)
            for (index in 0 until frameSize) {
                val sourceIndex = frameStart + index
                if (sourceIndex < padded.size) real[index] = padded[sourceIndex] * window[index]
            }
            fft(real, imaginary, inverse = false)

            prefix[0] = 0.0
            for (bin in 0 until binCount) {
                val magnitude = sqrt(
                    real[bin].toDouble() * real[bin] +
                        imaginary[bin].toDouble() * imaginary[bin],
                )
                logMagnitude[bin] = ln(max(magnitude, MIN_MAGNITUDE)).toFloat()
                prefix[bin + 1] = prefix[bin] + logMagnitude[bin]
            }
            for (bin in 0 until binCount) {
                val start = (bin - smoothingRadius).coerceAtLeast(0)
                val end = (bin + smoothingRadius + 1).coerceAtMost(binCount)
                envelope[bin] = ((prefix[end] - prefix[start]) / (end - start)).toFloat()
            }
            for (bin in 0 until binCount) {
                val sourceBin = bin / factor
                val shiftedEnvelope = interpolate(envelope, sourceBin)
                val envelopeDelta = (shiftedEnvelope - envelope[bin])
                    .coerceIn(-maximumEnvelopeDelta, maximumEnvelopeDelta)
                val gain = exp(envelopeDelta)
                real[bin] *= gain
                imaginary[bin] *= gain
                if (bin in 1 until frameSize / 2) {
                    real[frameSize - bin] = real[bin]
                    imaginary[frameSize - bin] = -imaginary[bin]
                }
            }

            fft(real, imaginary, inverse = true)
            for (index in 0 until frameSize) {
                val destination = frameStart + index
                if (destination < accumulated.size) {
                    val windowValue = window[index]
                    accumulated[destination] += real[index] * windowValue
                    weights[destination] += windowValue * windowValue
                }
            }
        }

        return FloatArray(input.size) { index ->
            val sourceIndex = index + padding
            val weight = weights[sourceIndex]
            if (weight > MIN_WINDOW_WEIGHT) accumulated[sourceIndex] / weight else input[index]
        }
    }

    private fun interpolate(values: FloatArray, position: Double): Float {
        if (position <= 0.0) return values.first()
        if (position >= values.lastIndex) return values.last()
        val left = position.toInt()
        val fraction = (position - left).toFloat()
        return values[left] + (values[left + 1] - values[left]) * fraction
    }

    private fun applyEqualizer(
        samples: FloatArray,
        sampleRateHz: Int,
        lowDb: Float,
        midDb: Float,
        highDb: Float,
        isCancelled: () -> Boolean,
    ) {
        val filters = listOfNotNull(
            lowDb.takeIf { abs(it) >= EFFECT_EPSILON }?.let {
                Biquad.lowShelf(sampleRateHz, LOW_EQ_HZ.coerceBelowNyquist(sampleRateHz), it)
            },
            midDb.takeIf { abs(it) >= EFFECT_EPSILON }?.let {
                Biquad.peaking(sampleRateHz, MID_EQ_HZ.coerceBelowNyquist(sampleRateHz), MID_EQ_Q, it)
            },
            highDb.takeIf { abs(it) >= EFFECT_EPSILON }?.let {
                Biquad.highShelf(sampleRateHz, HIGH_EQ_HZ.coerceBelowNyquist(sampleRateHz), it)
            },
        )
        samples.indices.forEach { index ->
            if (index % SAMPLE_CANCELLATION_INTERVAL == 0) checkNotCancelled(isCancelled)
            var value = samples[index].toDouble()
            filters.forEach { filter -> value = filter.process(value) }
            samples[index] = value.toFloat()
        }
    }

    private fun applySaturation(
        samples: FloatArray,
        driveDb: Float,
        isCancelled: () -> Boolean,
    ) {
        val drive = 10.0.pow(driveDb / 20.0)
        val normalization = tanh(drive).coerceAtLeast(0.0001)
        samples.indices.forEach { index ->
            if (index % SAMPLE_CANCELLATION_INTERVAL == 0) checkNotCancelled(isCancelled)
            samples[index] = (tanh(samples[index] * drive) / normalization).toFloat()
        }
    }

    private fun preventClipping(samples: FloatArray) {
        var peak = 0f
        samples.forEach { peak = max(peak, abs(it)) }
        if (peak <= OUTPUT_PEAK) return
        val gain = OUTPUT_PEAK / peak
        samples.indices.forEach { samples[it] *= gain }
    }

    private fun fft(real: FloatArray, imaginary: FloatArray, inverse: Boolean) {
        val size = real.size
        var reversed = 0
        for (index in 1 until size) {
            var bit = size shr 1
            while (reversed and bit != 0) {
                reversed = reversed xor bit
                bit = bit shr 1
            }
            reversed = reversed xor bit
            if (index < reversed) {
                val realValue = real[index]
                real[index] = real[reversed]
                real[reversed] = realValue
                val imaginaryValue = imaginary[index]
                imaginary[index] = imaginary[reversed]
                imaginary[reversed] = imaginaryValue
            }
        }

        var length = 2
        while (length <= size) {
            val angle = (if (inverse) 2.0 else -2.0) * PI / length
            val stepReal = cos(angle)
            val stepImaginary = sin(angle)
            var start = 0
            while (start < size) {
                var twiddleReal = 1.0
                var twiddleImaginary = 0.0
                val half = length / 2
                for (offset in 0 until half) {
                    val even = start + offset
                    val odd = even + half
                    val oddReal = real[odd] * twiddleReal - imaginary[odd] * twiddleImaginary
                    val oddImaginary = real[odd] * twiddleImaginary + imaginary[odd] * twiddleReal
                    val evenReal = real[even].toDouble()
                    val evenImaginary = imaginary[even].toDouble()
                    real[even] = (evenReal + oddReal).toFloat()
                    imaginary[even] = (evenImaginary + oddImaginary).toFloat()
                    real[odd] = (evenReal - oddReal).toFloat()
                    imaginary[odd] = (evenImaginary - oddImaginary).toFloat()
                    val nextReal = twiddleReal * stepReal - twiddleImaginary * stepImaginary
                    twiddleImaginary = twiddleReal * stepImaginary + twiddleImaginary * stepReal
                    twiddleReal = nextReal
                }
                start += length
            }
            length = length shl 1
        }
        if (inverse) {
            for (index in real.indices) {
                real[index] /= size
                imaginary[index] /= size
            }
        }
    }

    private fun nextPowerOfTwo(value: Int): Int {
        var result = 1
        while (result < value) result = result shl 1
        return result
    }

    private fun Float.coerceBelowNyquist(sampleRateHz: Int): Float =
        coerceAtMost(sampleRateHz * 0.45f)

    private fun checkNotCancelled(isCancelled: () -> Boolean) {
        if (isCancelled()) throw CancellationException("Speech audio processing was cancelled.")
    }

    private class Biquad(
        private val b0: Double,
        private val b1: Double,
        private val b2: Double,
        private val a1: Double,
        private val a2: Double,
    ) {
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        fun process(input: Double): Double {
            val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = input
            y2 = y1
            y1 = output
            return output
        }

        companion object {
            fun peaking(sampleRateHz: Int, frequencyHz: Float, q: Double, gainDb: Float): Biquad {
                val amplitude = 10.0.pow(gainDb / 40.0)
                val omega = 2.0 * PI * frequencyHz / sampleRateHz
                val alpha = sin(omega) / (2.0 * q)
                return normalized(
                    b0 = 1.0 + alpha * amplitude,
                    b1 = -2.0 * cos(omega),
                    b2 = 1.0 - alpha * amplitude,
                    a0 = 1.0 + alpha / amplitude,
                    a1 = -2.0 * cos(omega),
                    a2 = 1.0 - alpha / amplitude,
                )
            }

            fun lowShelf(sampleRateHz: Int, frequencyHz: Float, gainDb: Float): Biquad {
                val amplitude = 10.0.pow(gainDb / 40.0)
                val omega = 2.0 * PI * frequencyHz / sampleRateHz
                val cosine = cos(omega)
                val alpha = sin(omega) / 2.0 * sqrt(2.0)
                val beta = 2.0 * sqrt(amplitude) * alpha
                return normalized(
                    b0 = amplitude * ((amplitude + 1) - (amplitude - 1) * cosine + beta),
                    b1 = 2 * amplitude * ((amplitude - 1) - (amplitude + 1) * cosine),
                    b2 = amplitude * ((amplitude + 1) - (amplitude - 1) * cosine - beta),
                    a0 = (amplitude + 1) + (amplitude - 1) * cosine + beta,
                    a1 = -2 * ((amplitude - 1) + (amplitude + 1) * cosine),
                    a2 = (amplitude + 1) + (amplitude - 1) * cosine - beta,
                )
            }

            fun highShelf(sampleRateHz: Int, frequencyHz: Float, gainDb: Float): Biquad {
                val amplitude = 10.0.pow(gainDb / 40.0)
                val omega = 2.0 * PI * frequencyHz / sampleRateHz
                val cosine = cos(omega)
                val alpha = sin(omega) / 2.0 * sqrt(2.0)
                val beta = 2.0 * sqrt(amplitude) * alpha
                return normalized(
                    b0 = amplitude * ((amplitude + 1) + (amplitude - 1) * cosine + beta),
                    b1 = -2 * amplitude * ((amplitude - 1) + (amplitude + 1) * cosine),
                    b2 = amplitude * ((amplitude + 1) + (amplitude - 1) * cosine - beta),
                    a0 = (amplitude + 1) - (amplitude - 1) * cosine + beta,
                    a1 = 2 * ((amplitude - 1) - (amplitude + 1) * cosine),
                    a2 = (amplitude + 1) - (amplitude - 1) * cosine - beta,
                )
            }

            private fun normalized(
                b0: Double,
                b1: Double,
                b2: Double,
                a0: Double,
                a1: Double,
                a2: Double,
            ) = Biquad(
                b0 = b0 / a0,
                b1 = b1 / a0,
                b2 = b2 / a0,
                a1 = a1 / a0,
                a2 = a2 / a0,
            )
        }
    }

    private companion object {
        const val EFFECT_EPSILON = 0.001f
        const val MIN_PITCH_SAMPLES = 256
        const val WSOLA_WINDOW_SECONDS = 0.04f
        const val WSOLA_SEARCH_SECONDS = 0.012f
        const val MIN_WSOLA_WINDOW = 256
        const val MAX_WSOLA_WINDOW = 2_048
        const val WSOLA_CORRELATION_STRIDE = 4
        const val WSOLA_SEARCH_STRIDE = 4
        const val SILENCE_ENERGY = 1e-12
        const val FORMANT_FRAME_SECONDS = 0.04f
        const val FORMANT_OVERLAP_DIVISOR = 4
        const val FORMANT_SMOOTHING_HZ = 280f
        const val MAX_FORMANT_GAIN_DB = 12f
        const val MIN_FFT_SIZE = 1_024
        const val MAX_FFT_SIZE = 2_048
        const val MIN_MAGNITUDE = 1e-8
        const val MIN_WINDOW_WEIGHT = 1e-6f
        const val LOW_EQ_HZ = 160f
        const val MID_EQ_HZ = 1_500f
        const val MID_EQ_Q = 0.8
        const val HIGH_EQ_HZ = 5_000f
        const val OUTPUT_PEAK = 0.98f
        const val CANCELLATION_CHECK_INTERVAL = 8
        const val SAMPLE_CANCELLATION_INTERVAL = 16_384
    }
}
