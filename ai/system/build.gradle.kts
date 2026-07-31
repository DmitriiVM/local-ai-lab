plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.ai.system"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":ai:api"))
    implementation(project(":core:di"))
    implementation(libs.kotlinx.coroutines.android)
}
