plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localailab.feature.assistant.api"
}

dependencies {
    api(project(":core:navigation"))
}
