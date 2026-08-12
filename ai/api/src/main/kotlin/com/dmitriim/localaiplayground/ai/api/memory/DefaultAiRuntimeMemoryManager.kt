package com.dmitriim.localaiplayground.ai.api.memory

import com.dmitriim.localaiplayground.ai.api.llm.ChatEngine
import com.dmitriim.localaiplayground.ai.api.stt.SpeechToTextEngine
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechEngine
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.di.ApplicationCoroutineScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<AiRuntimeMemoryManager>())
class DefaultAiRuntimeMemoryManager(
    private val chatEngine: ChatEngine,
    private val speechToTextEngine: SpeechToTextEngine,
    private val textToSpeechEngine: TextToSpeechEngine,
    @param:ApplicationCoroutineScope private val applicationScope: CoroutineScope,
) : AiRuntimeMemoryManager {
    private val lock = Any()
    private val evictionMutex = Mutex()
    private val leaseCounts = mutableMapOf<AiRuntimeKind, Int>()
    private val pendingEvictions = mutableMapOf<AiRuntimeKind, Job>()

    override fun acquire(runtimeKinds: Set<AiRuntimeKind>): AutoCloseable {
        require(runtimeKinds.isNotEmpty()) { "A runtime lease must own at least one runtime kind." }
        val ownedKinds = runtimeKinds.toSet()
        synchronized(lock) {
            ownedKinds.forEach { kind ->
                pendingEvictions.remove(kind)?.cancel()
                leaseCounts[kind] = leaseCounts.getOrDefault(kind, 0) + 1
            }
        }
        return RuntimeLease { release(ownedKinds) }
    }

    override fun evictAll() {
        synchronized(lock) {
            pendingEvictions.values.forEach(Job::cancel)
            pendingEvictions.clear()
        }
        applicationScope.launch(Dispatchers.Default) {
            evictionMutex.withLock {
                AiRuntimeKind.entries.forEach { kind -> runCatching { cancelAndUnload(kind) } }
            }
        }
    }

    private fun release(runtimeKinds: Set<AiRuntimeKind>) {
        synchronized(lock) {
            runtimeKinds.forEach { kind ->
                val remaining = leaseCounts.getOrDefault(kind, 0) - 1
                check(remaining >= 0) { "Runtime lease released more than once for $kind." }
                if (remaining == 0) {
                    leaseCounts.remove(kind)
                    scheduleEviction(kind)
                } else {
                    leaseCounts[kind] = remaining
                }
            }
        }
    }

    private fun scheduleEviction(kind: AiRuntimeKind) {
        pendingEvictions.remove(kind)?.cancel()
        pendingEvictions[kind] = applicationScope.launch(Dispatchers.Default) {
            try {
                delay(FEATURE_HANDOFF_GRACE_PERIOD_MS)
                val shouldEvict = synchronized(lock) {
                    leaseCounts.getOrDefault(kind, 0) == 0 && pendingEvictions[kind] === coroutineContext[Job]
                }
                if (shouldEvict) {
                    evictionMutex.withLock { cancelAndUnload(kind) }
                }
            } finally {
                synchronized(lock) {
                    if (pendingEvictions[kind] === coroutineContext[Job]) pendingEvictions.remove(kind)
                }
            }
        }
    }

    private fun cancelAndUnload(kind: AiRuntimeKind) {
        when (kind) {
            AiRuntimeKind.CHAT -> try {
                chatEngine.cancel()
            } finally {
                chatEngine.unload()
            }
            AiRuntimeKind.SPEECH_TO_TEXT -> try {
                speechToTextEngine.cancel()
            } finally {
                speechToTextEngine.unload()
            }
            AiRuntimeKind.TEXT_TO_SPEECH -> try {
                textToSpeechEngine.cancel()
            } finally {
                textToSpeechEngine.unload()
            }
        }
    }

    private class RuntimeLease(private val release: () -> Unit) : AutoCloseable {
        private val closed = AtomicBoolean(false)

        override fun close() {
            if (closed.compareAndSet(false, true)) release()
        }
    }

    private companion object {
        const val FEATURE_HANDOFF_GRACE_PERIOD_MS = 500L
    }
}
