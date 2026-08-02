plugins {
    id("local-ai.android.library")
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.core.di"
}

dependencies {
    api(libs.metro.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.metrox.viewmodel)
}
