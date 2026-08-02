plugins {
    id("local-ai.android.library")
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.ai.api"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:di"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.metro.runtime)
}
