plugins {
    `kotlin-dsl`
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

group = "com.dmitriim.localailab.buildlogic"

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "local-ai.android.application"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.android.AndroidApplicationConventionPlugin"
        }
        register("androidComposeLibrary") {
            id = "local-ai.android.compose-library"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.android.AndroidComposeLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "local-ai.android.library"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.android.AndroidLibraryConventionPlugin"
        }
        register("nativeRuntime") {
            id = "local-ai.native-runtime"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.android.NativeRuntimeConventionPlugin"
        }
        register("jvmLibrary") {
            id = "local-ai.jvm.library"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.jvm.JvmLibraryConventionPlugin"
        }
        register("sherpaOnnx") {
            id = "local-ai.sherpa-onnx"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.dependency.SherpaOnnxConventionPlugin"
        }
        register("featureBoundaries") {
            id = "local-ai.feature-boundaries"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.dependency.FeatureBoundaryConventionPlugin"
        }
        register("androidLint") {
            id = "local-ai.android-lint"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.android.AndroidLintConventionPlugin"
        }
        register("detekt") {
            id = "local-ai.detekt"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.quality.DetektConventionPlugin"
        }
        register("ktlint") {
            id = "local-ai.ktlint"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.quality.KtlintConventionPlugin"
        }
        register("staticAnalysis") {
            id = "local-ai.static-analysis"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.quality.StaticAnalysisConventionPlugin"
        }
        register("androidStaticAnalysis") {
            id = "local-ai.android-static-analysis"
            implementationClass =
                "com.dmitriim.localailab.buildlogic.quality.AndroidStaticAnalysisConventionPlugin"
        }
    }
}
