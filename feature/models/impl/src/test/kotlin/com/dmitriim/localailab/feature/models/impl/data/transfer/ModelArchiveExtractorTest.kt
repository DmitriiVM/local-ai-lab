package com.dmitriim.localailab.feature.models.impl.data.transfer

import com.dmitriim.localailab.core.model.library.CatalogArchiveFormat
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelArchiveExtractorTest {
    @Test
    fun extractsZipContentsBelowTheDeclaredRoot() = withWorkspace { archive, destination ->
        writeZip(archive, listOf(ArchiveEntry("bundle/weights.bin", "model")))

        extract(archive, destination, CatalogArchiveFormat.ZIP)

        assertEquals("model", File(destination, "weights.bin").readText())
    }

    @Test
    fun rejectsZipPathTraversalWithoutWritingOutsideDestination() = withWorkspace { archive, destination ->
        writeZip(archive, listOf(ArchiveEntry("bundle/../escaped.bin", "unsafe")))

        assertThrows(IllegalArgumentException::class.java) {
            extract(archive, destination, CatalogArchiveFormat.ZIP)
        }

        assertFalse(File(destination.parentFile, "escaped.bin").exists())
    }

    @Test
    fun rejectsZipBackslashTraversalWithoutWritingOutsideDestination() = withWorkspace { archive, destination ->
        writeZip(archive, listOf(ArchiveEntry("bundle\\..\\escaped.bin", "unsafe")))

        assertThrows(IllegalArgumentException::class.java) {
            extract(archive, destination, CatalogArchiveFormat.ZIP)
        }

        assertFalse(File(destination.parentFile, "escaped.bin").exists())
    }

    @Test
    fun rejectsZipEntriesOutsideTheDeclaredRoot() = withWorkspace { archive, destination ->
        writeZip(archive, listOf(ArchiveEntry("other/weights.bin", "unsafe")))

        assertThrows(IllegalArgumentException::class.java) {
            extract(archive, destination, CatalogArchiveFormat.ZIP)
        }
    }

    @Test
    fun rejectsEmptyZipArchives() = withWorkspace { archive, destination ->
        writeZip(archive, emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            extract(archive, destination, CatalogArchiveFormat.ZIP)
        }
    }

    @Test
    fun rejectsDuplicateTarEntries() = withWorkspace { archive, destination ->
        writeTarBzip2(
            archive,
            listOf(
                ArchiveEntry("bundle/weights.bin", "first"),
                ArchiveEntry("bundle/weights.bin", "second"),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            extract(archive, destination, CatalogArchiveFormat.TAR_BZIP2)
        }
    }

    @Test
    fun extractsTarBzip2ContentsBelowTheDeclaredRoot() = withWorkspace { archive, destination ->
        writeTarBzip2(archive, listOf(ArchiveEntry("bundle/weights.bin", "model")))

        extract(archive, destination, CatalogArchiveFormat.TAR_BZIP2)

        assertEquals("model", File(destination, "weights.bin").readText())
    }

    @Test
    fun rejectsUnsafeDeclaredRoot() = withWorkspace { archive, destination ->
        writeZip(archive, listOf(ArchiveEntry("bundle/weights.bin", "model")))

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                ModelArchiveExtractor.extract(
                    archive = archive,
                    destinationRoot = destination,
                    rootDirectory = "bundle/subdirectory",
                    format = CatalogArchiveFormat.ZIP,
                )
            }
        }
    }

    private fun extract(archive: File, destination: File, format: CatalogArchiveFormat) = runBlocking {
        ModelArchiveExtractor.extract(
            archive = archive,
            destinationRoot = destination,
            rootDirectory = "bundle",
            format = format,
        )
    }

    private fun writeZip(archive: File, entries: List<ArchiveEntry>) {
        ZipOutputStream(archive.outputStream().buffered()).use { zip ->
            entries.forEach { entry ->
                zip.putNextEntry(ZipEntry(entry.name))
                zip.write(entry.contents.toByteArray())
                zip.closeEntry()
            }
        }
    }

    private fun writeTarBzip2(archive: File, entries: List<ArchiveEntry>) {
        BZip2CompressorOutputStream(FileOutputStream(archive)).use { compressed ->
            TarArchiveOutputStream(compressed).use { tar ->
                entries.forEach { entry ->
                    val contents = entry.contents.toByteArray()
                    tar.putArchiveEntry(TarArchiveEntry(entry.name).apply { size = contents.size.toLong() })
                    tar.write(contents)
                    tar.closeArchiveEntry()
                }
                tar.finish()
            }
        }
    }

    private inline fun withWorkspace(block: (archive: File, destination: File) -> Unit) {
        val root = Files.createTempDirectory("model-archive").toFile()
        try {
            val archive = File(root, "archive")
            val destination = File(root, "destination").also(File::mkdirs)
            block(archive, destination)
        } finally {
            root.deleteRecursively()
        }
    }

    private data class ArchiveEntry(val name: String, val contents: String)
}
