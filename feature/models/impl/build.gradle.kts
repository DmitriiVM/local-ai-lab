import java.util.Properties

plugins {
    id("local-ai.android.compose-library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.feature.models.impl"
    buildFeatures {
        buildConfig = true
    }
    val localProperties = Properties().apply {
        rootProject.file("local.properties")
            .takeIf { it.isFile }
            ?.inputStream()
            ?.use(::load)
    }
    fun buildConfigString(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    buildTypes {
        debug {
            buildConfigField(
                "String",
                "HUGGING_FACE_ACCESS_TOKEN",
                buildConfigString(localProperties.getProperty("huggingFaceAccessToken").orEmpty()),
            )
        }
        release {
            buildConfigField("String", "HUGGING_FACE_ACCESS_TOKEN", "\"\"")
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":feature:models:api"))
    implementation(project(":ai:api"))
    implementation(project(":ai:runtime"))
    implementation(project(":core:model"))
    implementation(project(":core:di"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":source:database"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.commons.compress)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.metro.runtime)
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)
    testImplementation(libs.junit)
}
