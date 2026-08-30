package com.dmitriim.localailab.core.security

/** Persists a single string value that can only be read on this device. */
interface EncryptedStringStore {
    /** Whether a value has been persisted. */
    val hasValue: Boolean

    /** Returns the persisted value, or throws when the stored value cannot be read. */
    fun read(): String

    /** Encrypts and persists [value]. */
    fun write(value: String)

    /** Removes both the persisted value and the key that protects it. */
    fun clear()
}
