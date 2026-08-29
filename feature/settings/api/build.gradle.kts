plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localailab.feature.settings.api"
}

dependencies {
    api(project(":core:navigation"))
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
}
