package com.dmitriim.localailab.feature.models.impl.data.credentials

import android.app.Application
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.security.AndroidKeystoreEncryptedStringStore
import com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus
import com.dmitriim.localailab.feature.models.api.data.ModelDownloadCredentials
import com.dmitriim.localailab.feature.models.impl.BuildConfig
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Keeps a user token encrypted outside backups and exposes a debug-only fallback token. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ModelDownloadCredentials>())
class HuggingFaceTokenStore(
    application: Application,
) : ModelDownloadCredentials {
    private val encryptedTokenStore = AndroidKeystoreEncryptedStringStore(
        application = application,
        storageName = TOKEN_FILE_NAME,
        keyAlias = KEY_ALIAS,
    )
    private val debugToken = BuildConfig.HUGGING_FACE_ACCESS_TOKEN.trim()
    private val mutableStatus = MutableStateFlow(initialStatus())

    override val huggingFaceCredentialStatus: Flow<HuggingFaceCredentialStatus> = mutableStatus.asStateFlow()

    override suspend fun saveHuggingFaceToken(token: String): Result<Unit> = runCatching {
        val trimmedToken = token.trim()
        require(trimmedToken.isNotEmpty()) { "Enter a Hugging Face access token." }
        withContext(Dispatchers.IO) {
            validate(trimmedToken)
            encryptedTokenStore.write(trimmedToken)
            mutableStatus.value = HuggingFaceCredentialStatus.USER_CONFIGURED
        }
    }

    override suspend fun clearHuggingFaceToken() = withContext(Dispatchers.IO) {
        encryptedTokenStore.clear()
        mutableStatus.value = fallbackStatus()
    }

    internal fun tokenOrNull(): String? = runCatching {
        if (!encryptedTokenStore.hasValue) return@runCatching debugToken.ifBlank { null }
        encryptedTokenStore.read()
    }.getOrElse {
        encryptedTokenStore.clear()
        mutableStatus.value = fallbackStatus()
        debugToken.ifBlank { null }
    }

    private fun initialStatus() = if (encryptedTokenStore.hasValue) {
        HuggingFaceCredentialStatus.USER_CONFIGURED
    } else {
        fallbackStatus()
    }

    private fun fallbackStatus() = if (debugToken.isBlank()) {
        HuggingFaceCredentialStatus.MISSING
    } else {
        HuggingFaceCredentialStatus.DEBUG_CONFIGURED
    }

    private fun validate(token: String) {
        val connection = java.net.URI(WHO_AM_I_URL).toURL().openConnection() as java.net.HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Authorization", "Bearer $token")
            val status = connection.responseCode
            require(status in 200..299) {
                "Hugging Face rejected this token. Create a read-only token and try again."
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TOKEN_FILE_NAME = "hugging-face-token"
        const val KEY_ALIAS = "local-ai-playground.hugging-face-token"
        const val WHO_AM_I_URL = "https://huggingface.co/api/whoami-v2"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
