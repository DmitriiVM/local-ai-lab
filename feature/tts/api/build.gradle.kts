plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localailab.feature.tts.api"
}

dependencies {
    api(project(":ai:api"))
    api(project(":core:audio"))
    api(project(":core:model"))
    api(project(":core:navigation"))
    api(project(":core:performance"))
    api(libs.kotlinx.coroutines.core)
}
