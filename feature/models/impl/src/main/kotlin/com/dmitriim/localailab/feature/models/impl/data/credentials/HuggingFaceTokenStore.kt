package com.dmitriim.localailab.feature.models.impl.data.credentials

import android.app.Application
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus
import com.dmitriim.localailab.feature.models.api.data.ModelDownloadCredentials
import com.dmitriim.localailab.feature.models.impl.BuildConfig
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
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
    private val tokenFile = AtomicFile(application.noBackupFilesDir.resolve(TOKEN_FILE_NAME))
    private val debugToken = BuildConfig.HUGGING_FACE_ACCESS_TOKEN.trim()
    private val mutableStatus = MutableStateFlow(initialStatus())

    override val huggingFaceCredentialStatus: Flow<HuggingFaceCredentialStatus> = mutableStatus.asStateFlow()

    override suspend fun saveHuggingFaceToken(token: String): Result<Unit> = runCatching {
        val trimmedToken = token.trim()
        require(trimmedToken.isNotEmpty()) { "Enter a Hugging Face access token." }
        withContext(Dispatchers.IO) {
            validate(trimmedToken)
            writeEncrypted(trimmedToken)
            mutableStatus.value = HuggingFaceCredentialStatus.USER_CONFIGURED
        }
    }

    override suspend fun clearHuggingFaceToken() = withContext(Dispatchers.IO) {
        tokenFile.delete()
        keyStore().deleteEntry(KEY_ALIAS)
        mutableStatus.value = fallbackStatus()
    }

    internal fun tokenOrNull(): String? = runCatching {
        if (!tokenFile.baseFile.exists()) return@runCatching debugToken.ifBlank { null }
        decrypt(tokenFile.readFully().toString(Charsets.UTF_8))
    }.getOrElse {
        tokenFile.delete()
        mutableStatus.value = fallbackStatus()
        debugToken.ifBlank { null }
    }

    private fun initialStatus() = if (tokenFile.baseFile.exists()) {
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

    private fun writeEncrypted(token: String) {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key())
        }
        val encoded = listOf(
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(cipher.doFinal(token.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
        ).joinToString(SEPARATOR)
        val stream = tokenFile.startWrite()
        try {
            stream.write(encoded.toByteArray(Charsets.UTF_8))
            tokenFile.finishWrite(stream)
        } catch (error: Throwable) {
            tokenFile.failWrite(stream)
            throw error
        }
    }

    private fun decrypt(encoded: String): String {
        val parts = encoded.split(SEPARATOR, limit = 2)
        require(parts.size == 2) { "The saved Hugging Face token is unreadable." }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(parts[0], Base64.NO_WRAP)),
            )
        }
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val store = keyStore()
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }

    private fun keyStore() = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private companion object {
        const val TOKEN_FILE_NAME = "hugging-face-token"
        const val KEY_ALIAS = "local-ai-playground.hugging-face-token"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val SEPARATOR = ":"
        const val WHO_AM_I_URL = "https://huggingface.co/api/whoami-v2"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
