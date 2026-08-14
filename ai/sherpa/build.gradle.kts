plugins {
    id("local-ai.android.library")
    id("local-ai.sherpa-onnx")
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.ai.sherpa"
    defaultConfig {
        ndk {
            abiFilters += "arm64-v8a"
        }
    }
}

dependencies {
    api(project(":ai:api"))
    implementation(project(":core:di"))
    implementation(libs.sherpa.onnx)
    implementation(libs.kotlinx.coroutines.android)
}
