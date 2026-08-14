import java.util.Properties

plugins {
    id("local-ai.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localailab.source.models"
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
    implementation(project(":ai:api"))
    implementation(project(":core:di"))
    implementation(project(":core:model"))
    implementation(project(":source:database"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.commons.compress)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.metro.runtime)
    testImplementation(libs.junit)
}
