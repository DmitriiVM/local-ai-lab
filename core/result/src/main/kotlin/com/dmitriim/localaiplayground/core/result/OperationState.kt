package com.dmitriim.localaiplayground.core.result

sealed interface OperationState<out T> {
    data object Idle : OperationState<Nothing>
    data class Preparing(val message: String) : OperationState<Nothing>
    data class Running(
        val message: String,
        val completed: Long? = null,
        val total: Long? = null,
    ) : OperationState<Nothing>

    data class Cancelling(val message: String) : OperationState<Nothing>
    data class Completed<T>(val value: T) : OperationState<T>
    data class Error(val error: DomainError) : OperationState<Nothing>
}
