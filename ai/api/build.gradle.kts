plugins {
    id("local-ai.jvm.library")
    alias(libs.plugins.metro)
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:di"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.metro.runtime)
    testImplementation(libs.junit)
}
