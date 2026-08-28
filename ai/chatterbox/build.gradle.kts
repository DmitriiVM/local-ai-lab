plugins {
    id("local-ai.android.library")
    id("local-ai.native-runtime")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.ai.chatterbox"
}

dependencies {
    api(project(":ai:api"))
    implementation(project(":ai:runtime"))
    implementation(project(":core:di"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.onnxruntime.android)
    testImplementation(libs.junit)
}
