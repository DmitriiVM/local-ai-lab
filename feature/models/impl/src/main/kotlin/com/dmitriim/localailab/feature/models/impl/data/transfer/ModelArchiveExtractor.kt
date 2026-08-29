package com.dmitriim.localailab.feature.models.impl.data.transfer

import com.dmitriim.localailab.ai.api.model.library.CatalogArchiveFormat
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

/** Extracts a verified catalog archive only when every entry stays inside its declared root. */
internal object ModelArchiveExtractor {
    suspend fun extract(
        archive: File,
        destinationRoot: File,
        rootDirectory: String,
        format: CatalogArchiveFormat,
    ) {
        validateRootDirectory(rootDirectory)
        when (format) {
            CatalogArchiveFormat.TAR_BZIP2 -> extractTarBzip2(archive, destinationRoot, rootDirectory)
            CatalogArchiveFormat.ZIP -> extractZip(archive, destinationRoot, rootDirectory)
        }
    }

    private suspend fun extractTarBzip2(archive: File, destinationRoot: File, rootDirectory: String) {
        var extractedFiles = 0
        BZip2CompressorInputStream(archive.inputStream().buffered()).use { compressed ->
            TarArchiveInputStream(compressed).use { tar ->
                while (true) {
                    coroutineContext.ensureActive()
                    val entry = tar.nextEntry ?: break
                    val destination = entryDestination(entry.name, destinationRoot, rootDirectory, entry.isDirectory)
                    when {
                        entry.isDirectory -> createDirectory(destination)
                        entry.isFile -> {
                            writeFile(destination) { buffer -> tar.read(buffer) }
                            extractedFiles += 1
                        }
                        else -> error("The model archive contains an unsupported entry.")
                    }
                }
            }
        }
        require(extractedFiles > 0) { "The model archive does not contain any files." }
    }

    private suspend fun extractZip(archive: File, destinationRoot: File, rootDirectory: String) {
        var extractedFiles = 0
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                coroutineContext.ensureActive()
                val entry = zip.nextEntry ?: break
                val destination = entryDestination(entry.name, destinationRoot, rootDirectory, entry.isDirectory)
                if (entry.isDirectory) {
                    createDirectory(destination)
                } else {
                    writeFile(destination) { buffer -> zip.read(buffer) }
                    extractedFiles += 1
                }
                zip.closeEntry()
            }
        }
        require(extractedFiles > 0) { "The model archive does not contain any files." }
    }

    private fun entryDestination(
        entryName: String,
        destinationRoot: File,
        rootDirectory: String,
        directory: Boolean,
    ): File {
        val archivePath = entryName.normalizedArchivePath()
        require(archivePath == rootDirectory || archivePath.startsWith("$rootDirectory/")) {
            "The model archive contains an unexpected path."
        }
        val relativePath = archivePath.removePrefix(rootDirectory).removePrefix("/").trimEnd('/')
        if (relativePath.isBlank()) {
            require(directory) { "The model archive contains an unsafe root entry." }
            return destinationRoot
        }
        require(relativePath.split('/').none { it in setOf("", ".", "..") }) {
            "The model archive contains an unsafe path."
        }
        return File(destinationRoot, relativePath).also { destination ->
            require(destination.canonicalPath.startsWith(destinationRoot.canonicalPath + File.separator)) {
                "The model archive contains an unsafe path."
            }
        }
    }

    private fun createDirectory(destination: File) {
        require(destination.mkdirs() || destination.isDirectory) {
            "Could not create an extracted model directory."
        }
    }

    private suspend fun writeFile(destination: File, readChunk: (ByteArray) -> Int) {
        require(!destination.exists()) { "The model archive contains duplicate paths." }
        val parent = requireNotNull(destination.parentFile)
        createDirectory(parent)
        FileOutputStream(destination).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                coroutineContext.ensureActive()
                val read = readChunk(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
            }
        }
    }

    private fun validateRootDirectory(rootDirectory: String) {
        require(rootDirectory.isNotBlank() && !rootDirectory.contains('/') && !rootDirectory.contains('\\')) {
            "The catalog archive root is unsafe."
        }
    }

    /** Tar tools commonly emit a harmless "./" prefix; root and traversal checks run after removing it. */
    private fun String.normalizedArchivePath(): String = replace('\\', '/').removePrefix("./")
}
