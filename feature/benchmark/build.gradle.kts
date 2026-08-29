plugins {
    id("local-ai.android.compose-library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.feature.benchmark"
}

dependencies {
    implementation(project(":core:di"))
    implementation(project(":core:audio"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":core:performance"))
    implementation(project(":core:operation"))
    implementation(project(":core:ui"))
    implementation(project(":feature:runs:api"))
    implementation(project(":ai:api"))
    implementation(project(":ai:runtime"))
    implementation(project(":feature:stt:api"))
    implementation(project(":feature:models:api"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.metro.runtime)
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
