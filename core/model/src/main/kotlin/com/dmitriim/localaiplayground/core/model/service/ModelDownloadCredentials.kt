package com.dmitriim.localaiplayground.core.model.service

import kotlinx.coroutines.flow.Flow

/** Manages the user credentials required by selected gated model downloads. */
interface ModelDownloadCredentials {
    val huggingFaceCredentialStatus: Flow<HuggingFaceCredentialStatus>

    suspend fun saveHuggingFaceToken(token: String): Result<Unit>
    suspend fun clearHuggingFaceToken()
}

enum class HuggingFaceCredentialStatus {
    MISSING,
    USER_CONFIGURED,
    DEBUG_CONFIGURED,
}
