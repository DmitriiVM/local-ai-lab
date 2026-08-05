plugins {
    id("local-ai.android.library")
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.core.voice"

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":ai:api"))
    implementation(project(":core:audio"))
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.metro.runtime)
    testImplementation(libs.junit)
}
