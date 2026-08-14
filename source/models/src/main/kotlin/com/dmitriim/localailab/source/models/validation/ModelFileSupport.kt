package com.dmitriim.localailab.source.models.validation

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

internal fun File.sha256(): String = FileInputStream(this).use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(FILE_IO_BUFFER_BYTES)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

internal fun Long.toReadableBytes(): String = "%.1f GiB".format(toDouble() / 1024 / 1024 / 1024)

private const val FILE_IO_BUFFER_BYTES = 256 * 1024
