plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localailab.feature.playground.api"
}

dependencies {
    api(project(":core:navigation"))
}
