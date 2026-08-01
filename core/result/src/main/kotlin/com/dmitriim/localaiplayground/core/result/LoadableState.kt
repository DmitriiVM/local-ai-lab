package com.dmitriim.localaiplayground.core.result

sealed interface LoadableState<out T> {
    data object Loading : LoadableState<Nothing>
    data class Content<T>(val value: T) : LoadableState<T>
    data class Empty(val title: String, val explanation: String) : LoadableState<Nothing>
    data class Unsupported(val title: String, val explanation: String) : LoadableState<Nothing>
    data class Error(val error: DomainError) : LoadableState<Nothing>
}
