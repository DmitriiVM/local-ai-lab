package com.dmitriim.localaiplayground.buildlogic.dependency

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

@CacheableTransform
abstract class StripOnnxRuntimeFromAarTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputAar = inputArtifact.get().asFile
        if (!inputAar.name.startsWith("sherpa-onnx-")) {
            outputs.file(inputAar)
            return
        }
        val outputAar = outputs.file("${inputAar.nameWithoutExtension}-without-onnxruntime.aar")

        ZipFile(inputAar).use { input ->
            ZipOutputStream(outputAar.outputStream().buffered()).use { output ->
                input.copyEntriesTo(output)
            }
        }
    }

    private fun ZipFile.copyEntriesTo(output: ZipOutputStream) {
        entries().asSequence()
            .filterNot { it.name.matches(ONNX_RUNTIME_NATIVE_LIBRARY) }
            .forEach { entry ->
                output.putNextEntry(entry.copyForOutput())
                if (!entry.isDirectory) {
                    getInputStream(entry).use { it.copyTo(output) }
                }
                output.closeEntry()
            }
    }

    private fun ZipEntry.copyForOutput(): ZipEntry = ZipEntry(name).also { copy ->
        copy.time = time
        copy.comment = comment
        copy.extra = extra
        copy.method = ZipEntry.DEFLATED
    }

    private companion object {
        val ONNX_RUNTIME_NATIVE_LIBRARY = Regex("jni/[^/]+/libonnxruntime\\.so")
    }
}
