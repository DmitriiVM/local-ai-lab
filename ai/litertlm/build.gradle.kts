plugins {
    id("local-ai.android.library")
    id("local-ai.native-runtime")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.ai.litertlm"
}

dependencies {
    api(project(":ai:api"))
    implementation(project(":core:di"))
    implementation(libs.androidx.tracing)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.litertlm.android)
    testImplementation(libs.junit)
}
