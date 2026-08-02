plugins {
    `kotlin-dsl`
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

group = "com.dmitriim.localaiplayground.buildlogic"

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
                "com.dmitriim.localaiplayground.buildlogic.android.AndroidApplicationConventionPlugin"
        }
        register("androidComposeLibrary") {
            id = "local-ai.android.compose-library"
            implementationClass =
                "com.dmitriim.localaiplayground.buildlogic.android.AndroidComposeLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "local-ai.android.library"
            implementationClass =
                "com.dmitriim.localaiplayground.buildlogic.android.AndroidLibraryConventionPlugin"
        }
        register("androidLint") {
            id = "local-ai.android-lint"
            implementationClass =
                "com.dmitriim.localaiplayground.buildlogic.android.AndroidLintConventionPlugin"
        }
        register("detekt") {
            id = "local-ai.detekt"
            implementationClass =
                "com.dmitriim.localaiplayground.buildlogic.quality.DetektConventionPlugin"
        }
        register("ktlint") {
            id = "local-ai.ktlint"
            implementationClass =
                "com.dmitriim.localaiplayground.buildlogic.quality.KtlintConventionPlugin"
        }
        register("staticAnalysis") {
            id = "local-ai.static-analysis"
            implementationClass =
                "com.dmitriim.localaiplayground.buildlogic.quality.StaticAnalysisConventionPlugin"
        }
        register("androidStaticAnalysis") {
            id = "local-ai.android-static-analysis"
            implementationClass =
                "com.dmitriim.localaiplayground.buildlogic.quality.AndroidStaticAnalysisConventionPlugin"
        }
    }
}
