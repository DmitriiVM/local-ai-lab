plugins {
    id("local-ai.android.library")
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.ai.system"
}

dependencies {
    implementation(project(":ai:api"))
    implementation(project(":core:di"))
    implementation(libs.kotlinx.coroutines.android)
}
