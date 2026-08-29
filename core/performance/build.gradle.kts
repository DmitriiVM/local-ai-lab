plugins {
    id("local-ai.android.library")
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.core.performance"
}

dependencies {
    implementation(project(":core:di"))
    implementation(libs.metro.runtime)
}
