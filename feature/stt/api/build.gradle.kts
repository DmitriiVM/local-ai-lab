plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localailab.feature.stt.api"
}

dependencies {
    api(project(":core:audio"))
    api(project(":ai:api"))
    api(project(":core:navigation"))
    api(libs.kotlinx.coroutines.core)
}
