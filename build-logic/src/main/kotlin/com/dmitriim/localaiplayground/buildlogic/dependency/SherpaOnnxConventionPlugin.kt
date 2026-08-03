package com.dmitriim.localaiplayground.buildlogic.dependency

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.kotlin.dsl.registerTransform

class SherpaOnnxConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        dependencies.attributesSchema.attribute(ONNX_RUNTIME_STRIPPED_ATTRIBUTE)
        dependencies.artifactTypes.maybeCreate("aar").attributes.attribute(
            ONNX_RUNTIME_STRIPPED_ATTRIBUTE,
            false,
        )

        configurations.configureEach {
            if (isCanBeResolved) {
                attributes.attribute(ONNX_RUNTIME_STRIPPED_ATTRIBUTE, true)
            }
        }

        dependencies.registerTransform(StripOnnxRuntimeFromAarTransform::class) {
            from.attribute(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                "aar",
            )
            from.attribute(ONNX_RUNTIME_STRIPPED_ATTRIBUTE, false)
            to.attribute(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                "aar",
            )
            to.attribute(ONNX_RUNTIME_STRIPPED_ATTRIBUTE, true)
        }
    }

    companion object {
        private val ONNX_RUNTIME_STRIPPED_ATTRIBUTE: Attribute<Boolean> =
            Attribute.of(
                "com.dmitriim.localaiplayground.sherpa-onnx-runtime-stripped",
                Boolean::class.javaObjectType,
            )
    }
}
