plugins {
    id("local-ai.android.library")
    alias(libs.plugins.metro)
}

dependencies {
    implementation(project(":core:di"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.metro.runtime)
    testImplementation(libs.junit)
}

android {
    namespace = "com.dmitriim.localailab.core.audio"
}
