package com.dmitriim.localailab.core.result

import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultForegroundOperationCoordinator : ForegroundOperationCoordinator {
    private val nextId = AtomicLong()
    private val operations = ConcurrentHashMap<Long, (ForegroundOperationInterruption) -> Unit>()

    override fun register(cancel: () -> Unit): AutoCloseable = registerInterruptionHandler(
        onInterrupt = { cancel() },
    )

    override fun registerInterruptionHandler(
        onInterrupt: (ForegroundOperationInterruption) -> Unit,
    ): AutoCloseable {
        val id = nextId.incrementAndGet()
        operations[id] = onInterrupt
        return AutoCloseable { operations.remove(id) }
    }

    override fun interruptActiveOperations(interruption: ForegroundOperationInterruption) {
        operations.values.toList().forEach { onInterrupt -> onInterrupt(interruption) }
        operations.clear()
    }
}
