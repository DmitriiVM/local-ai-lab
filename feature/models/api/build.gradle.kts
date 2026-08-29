plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmitriim.localailab.feature.models.api"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:navigation"))
}
