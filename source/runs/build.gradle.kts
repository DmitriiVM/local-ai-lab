plugins {
    id("local-ai.android.library")
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.source.runs"
}

dependencies {
    implementation(project(":core:di"))
    implementation(project(":core:model"))
    implementation(project(":source:database"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.metro.runtime)
    testImplementation(libs.junit)
}
