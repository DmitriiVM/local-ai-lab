plugins {
    id("local-ai.android.compose-library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.feature.models"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:di"))
    implementation(project(":core:navigation"))
    implementation(project(":core:result"))
    implementation(project(":core:ui"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)
}
