plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.source.models"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":ai:api"))
    implementation(project(":core:di"))
    implementation(project(":core:model"))
    implementation(project(":source:database"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.commons.compress)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.metro.runtime)
}
