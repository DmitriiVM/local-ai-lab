plugins {
    id("local-ai.android.compose-library")
}

android {
    namespace = "com.dmitriim.localaiplayground.core.ui"
}

dependencies {
    implementation(project(":core:result"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
}
