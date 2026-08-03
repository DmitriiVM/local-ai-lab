plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.ai.litertlm"
    defaultConfig {
        ndk { abiFilters += "arm64-v8a" }
    }
}

dependencies {
    api(project(":ai:api"))
    implementation(project(":core:di"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.litertlm.android)
    testImplementation(libs.junit)
}
