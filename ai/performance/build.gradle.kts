plugins {
    id("local-ai.android.library")
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.ai.performance"
}

dependencies {
    implementation(project(":ai:api"))
    implementation(project(":core:di"))
    implementation(project(":core:performance"))
    implementation(libs.androidx.tracing)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.metro.runtime)
}
