plugins {
    id("local-ai.android.compose-library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.feature.settings"
}

dependencies {
    implementation(project(":core:audio"))
    implementation(project(":core:di"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":core:result"))
    implementation(project(":core:ui"))
    implementation(project(":source:runs"))
    implementation(project(":source:settings"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)
}
