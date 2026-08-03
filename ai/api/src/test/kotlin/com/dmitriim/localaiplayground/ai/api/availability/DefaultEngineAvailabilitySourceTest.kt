package com.dmitriim.localaiplayground.ai.api.availability

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.ComputePreference
import com.dmitriim.localaiplayground.core.model.engine.EngineAvailability
import com.dmitriim.localaiplayground.core.model.engine.EngineDescriptor
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.engine.EngineKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultEngineAvailabilitySourceTest {
    @Test
    fun `refresh publishes results sorted by display name`() = runBlocking {
        val source = DefaultEngineAvailabilitySource(setOf(FakeProbe("Zulu"), FakeProbe("Alpha")))

        source.refresh()

        assertEquals(listOf("Alpha", "Zulu"), source.availability.value.map { it.descriptor.displayName })
    }

    @Test
    fun `failed refresh preserves previously published availability`() = runBlocking {
        val probe = FakeProbe("Ready")
        val source = DefaultEngineAvailabilitySource(setOf(probe))
        source.refresh()
        probe.failure = IllegalStateException("runtime unavailable")

        val outcome = runCatching { source.refresh() }

        assertTrue(outcome.isFailure)
        assertEquals(listOf("Ready"), source.availability.value.map { it.descriptor.displayName })
    }

    @Test
    fun `refresh starts all probes before waiting for their results`() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val firstStarted = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val source = DefaultEngineAvailabilitySource(
            setOf(
                BlockingProbe("First", firstStarted, release),
                BlockingProbe("Second", secondStarted, release),
            ),
        )

        val refresh = async { source.refresh() }
        withTimeout(1_000) {
            firstStarted.await()
            secondStarted.await()
        }
        release.complete(Unit)
        refresh.await()
    }

    private class FakeProbe(displayName: String) : EngineAvailabilityProbe {
        private val descriptor = EngineDescriptor(
            id = EngineId(displayName.lowercase()),
            displayName = displayName,
            kind = EngineKind.CUSTOM,
            capabilities = setOf(AiCapability.CHAT),
            bundledRuntime = true,
        )
        var failure: Throwable? = null

        override suspend fun probe(): EngineAvailability {
            failure?.let { throw it }
            return EngineAvailability.Available(descriptor, ComputePreference.CPU, "Ready")
        }
    }

    private class BlockingProbe(
        displayName: String,
        private val started: CompletableDeferred<Unit>,
        private val release: CompletableDeferred<Unit>,
    ) : EngineAvailabilityProbe {
        private val descriptor = EngineDescriptor(
            id = EngineId(displayName.lowercase()),
            displayName = displayName,
            kind = EngineKind.CUSTOM,
            capabilities = setOf(AiCapability.CHAT),
            bundledRuntime = true,
        )

        override suspend fun probe(): EngineAvailability {
            started.complete(Unit)
            release.await()
            return EngineAvailability.Available(descriptor, ComputePreference.CPU, "Ready")
        }
    }
}
