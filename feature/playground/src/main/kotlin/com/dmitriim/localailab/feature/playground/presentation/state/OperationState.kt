package com.dmitriim.localailab.feature.playground.presentation.state

sealed interface OperationState<out T> {
    data class Preparing(val message: String) : OperationState<Nothing>
    data class Completed<T>(val value: T) : OperationState<T>
    data class Error(val error: DomainError) : OperationState<Nothing>
}
