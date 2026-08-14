package com.dmitriim.localailab.core.result

interface ForegroundOperationCoordinator {
    fun register(cancel: () -> Unit): AutoCloseable

    fun interruptActiveOperations()
}
