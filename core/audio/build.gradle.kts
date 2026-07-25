plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.metro)
}

dependencies {
    implementation(project(":core:di"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.metro.runtime)
}

android {
    namespace = "com.dmitriim.localaiplayground.core.audio"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    defaultConfig { minSdk = 26 }
}
