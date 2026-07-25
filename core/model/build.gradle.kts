plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localaiplayground.core.model"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    api(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.coroutines.android)
}
