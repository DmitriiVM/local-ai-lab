plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localaiplayground.core.model"
}

dependencies {
    api(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.coroutines.android)
}
