plugins {
    id("local-ai.android.compose-library")
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.core.result"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:di"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.metro.runtime)
    testImplementation(libs.junit)
}
