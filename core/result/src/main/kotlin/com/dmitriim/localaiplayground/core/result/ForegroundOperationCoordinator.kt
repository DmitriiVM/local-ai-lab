package com.dmitriim.localaiplayground.core.result

interface ForegroundOperationCoordinator {
    fun register(cancel: () -> Unit): AutoCloseable

    fun interruptActiveOperations()
}
