plugins {
    id("local-ai.android.compose-library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localailab.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(project(":core:model"))
    api(libs.kotlinx.serialization.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.ui)
}
