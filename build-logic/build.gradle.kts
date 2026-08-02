plugins {
    `kotlin-dsl`
}

group = "com.dmitriim.localaiplayground.buildlogic"

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "local-ai.android.application"
            implementationClass =
                "com.dmitriim.localaiplayground.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidComposeLibrary") {
            id = "local-ai.android.compose-library"
            implementationClass =
                "com.dmitriim.localaiplayground.buildlogic.AndroidComposeLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "local-ai.android.library"
            implementationClass =
                "com.dmitriim.localaiplayground.buildlogic.AndroidLibraryConventionPlugin"
        }
    }
}
