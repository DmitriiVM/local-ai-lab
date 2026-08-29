package com.dmitriim.localailab.core.operation

interface ForegroundOperationCoordinator {
    fun register(cancel: () -> Unit): AutoCloseable

    fun registerInterruptionHandler(
        onInterrupt: (ForegroundOperationInterruption) -> Unit,
    ): AutoCloseable

    fun interruptActiveOperations(
        interruption: ForegroundOperationInterruption = ForegroundOperationInterruption.APP_BACKGROUNDED,
    )
}
