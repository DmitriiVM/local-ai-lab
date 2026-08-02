plugins {
    id("local-ai.android.compose-library")
}

android {
    namespace = "com.dmitriim.localaiplayground.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.ui)
}
