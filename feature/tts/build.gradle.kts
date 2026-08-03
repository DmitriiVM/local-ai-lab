plugins {
    id("local-ai.android.compose-library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.feature.tts"

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":ai:api"))
    implementation(project(":core:audio"))
    implementation(project(":core:di"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":core:result"))
    implementation(project(":core:voice"))
    implementation(project(":source:runs"))
    implementation(project(":source:settings"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)
    testImplementation(libs.junit)
}
