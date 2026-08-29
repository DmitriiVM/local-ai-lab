plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localailab.feature.benchmark.api"
}

dependencies {
    api(project(":ai:api"))
    api(project(":core:audio"))
    api(project(":core:navigation"))
    api(libs.kotlinx.coroutines.core)
}
