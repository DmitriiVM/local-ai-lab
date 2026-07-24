plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.dmitriim.localaiplayground.core.metrics"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    defaultConfig { minSdk = 26 }
}
