package com.dmitriim.localailab.source.models.library

import com.dmitriim.localailab.ai.api.model.ModelImportDefinition
import com.dmitriim.localailab.ai.api.model.ModelImportFileDefinition
import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelId
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelImportPolicyTest {
    @Test
    fun `safe file names reject path traversal`() {
        assertTrue(ModelImportPolicy.isSafeFileName("weights.bin"))
        assertFalse(ModelImportPolicy.isSafeFileName("../weights.bin"))
        assertFalse(ModelImportPolicy.isSafeFileName("nested/weights.bin"))
        assertFalse(ModelImportPolicy.isSafeFileName("nested\\weights.bin"))
        assertFalse(ModelImportPolicy.isSafeFileName(".."))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `destination rejects traversal outside the import root`() {
        val root = Files.createTempDirectory("model-import").toFile()
        try {
            ModelImportPolicy.destination(root, "../outside.bin")
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `role specs resolve exact files extensions and directories`() {
        val definition = ModelImportDefinition(
            displayName = "Test model",
            format = ModelFormat.ONNX,
            files = listOf(
                ModelImportFileDefinition(ModelFileRole("CONFIG"), relativePath = "config.json"),
                ModelImportFileDefinition(ModelFileRole("WEIGHTS"), extension = ".onnx"),
                ModelImportFileDefinition(ModelFileRole("VOCAB"), relativePath = "vocab", directory = true),
            ),
        )

        val files = ModelImportPolicy.roleSpecs(
            definition,
            listOf("config.json", "weights.ONNX", "vocab/tokens.txt"),
        )

        assertEquals(listOf("config.json", "weights.ONNX", "vocab"), files.map { it.relativePath })
        assertTrue(files.last().directory)
    }

    @Test
    fun `directory names remain contained and deterministic`() {
        assertEquals("model_name_1", ModelImportPolicy.directoryName(ModelId("model name/1")))
    }
}
