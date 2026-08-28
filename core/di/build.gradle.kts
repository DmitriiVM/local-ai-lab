plugins {
    id("local-ai.jvm.library")
    alias(libs.plugins.metro)
}

dependencies {
    api(libs.metro.runtime)
    implementation(libs.metrox.viewmodel)
}
