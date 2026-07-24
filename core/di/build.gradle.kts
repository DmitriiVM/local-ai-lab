plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.core.di"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    api(libs.metro.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.metrox.viewmodel)
}
