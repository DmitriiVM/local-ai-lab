plugins {
    id("local-ai.android.application")
    id("local-ai.native-runtime")
    id("local-ai.sherpa-onnx")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab"
    defaultConfig {
        applicationId = "com.dmitriim.localailab"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
        create("profile") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "PROFILE_BUILD", "true")
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":ai:api"))
    implementation(project(":ai:runtime"))
    implementation(project(":ai:chatterbox"))
    implementation(project(":ai:llamacpp"))
    implementation(project(":ai:litertlm"))
    implementation(project(":ai:sherpa"))
    implementation(project(":ai:vosk"))
    implementation(project(":ai:system"))
    implementation(project(":core:audio"))
    implementation(project(":core:di"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":core:performance"))
    implementation(project(":core:operation"))
    implementation(project(":core:ui"))
    implementation(project(":feature:assistant"))
    implementation(project(":feature:benchmark"))
    implementation(project(":feature:device"))
    implementation(project(":feature:models:api"))
    implementation(project(":feature:models:impl"))
    implementation(project(":feature:playground"))
    implementation(project(":feature:runs:api"))
    implementation(project(":feature:runs:impl"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:settings:impl"))
    implementation(project(":feature:stt:api"))
    implementation(project(":feature:stt:impl"))
    implementation(project(":feature:tts:api"))
    implementation(project(":feature:tts:impl"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.metro.runtime)
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)
    implementation(libs.onnxruntime.android)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
