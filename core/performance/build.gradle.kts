plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.core.performance"
}

dependencies {
    api(project(":ai:api"))
    api(project(":core:audio"))
    api(project(":core:model"))
    implementation(project(":core:di"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.tracing)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.metro.runtime)
}
