plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.ai.vosk"
    defaultConfig {
        ndk { abiFilters += "arm64-v8a" }
    }
}

dependencies {
    api(project(":ai:api"))
    implementation(project(":core:di"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.vosk.android)
}
