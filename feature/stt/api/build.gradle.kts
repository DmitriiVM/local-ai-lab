plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localailab.feature.stt.api"
}

dependencies {
    api(project(":core:audio"))
    api(project(":core:model"))
    api(project(":core:navigation"))
    api(project(":core:performance"))
    api(libs.kotlinx.coroutines.core)
}
