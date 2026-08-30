package com.dmitriim.localailab.core.security

import android.app.Application
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** An [EncryptedStringStore] backed by an Android Keystore AES-GCM key and a no-backup file. */
class AndroidKeystoreEncryptedStringStore(
    application: Application,
    storageName: String,
    private val keyAlias: String,
) : EncryptedStringStore {
    private val encryptedFile = AtomicFile(application.noBackupFilesDir.resolve(storageName))

    override val hasValue: Boolean
        get() = encryptedFile.baseFile.exists()

    override fun read(): String {
        val parts = encryptedFile.readFully().toString(Charsets.UTF_8).split(SEPARATOR, limit = 2)
        require(parts.size == 2) { "The encrypted value is unreadable." }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(parts[0], Base64.NO_WRAP)),
            )
        }
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    override fun write(value: String) {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key())
        }
        val encoded = listOf(
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
        ).joinToString(SEPARATOR)
        val stream = encryptedFile.startWrite()
        try {
            stream.write(encoded.toByteArray(Charsets.UTF_8))
            encryptedFile.finishWrite(stream)
        } catch (error: Throwable) {
            encryptedFile.failWrite(stream)
            throw error
        }
    }

    override fun clear() {
        encryptedFile.delete()
        keyStore().deleteEntry(keyAlias)
    }

    private fun key(): SecretKey {
        val store = keyStore()
        (store.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).apply {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
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
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val SEPARATOR = ":"
    }
}
