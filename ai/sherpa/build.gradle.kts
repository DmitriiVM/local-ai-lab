plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.metro)
}

val strippedSherpaAar by tasks.registering(Zip::class) {
    val sourceAar = layout.projectDirectory.file("libs/sherpa-onnx-1.13.4.aar")
    from(zipTree(sourceAar))
    exclude("jni/**/libonnxruntime.so")
    archiveFileName.set("sherpa-onnx-1.13.4-no-ort.aar")
    destinationDirectory.set(layout.buildDirectory.dir("stripped-aar"))
}

android {
    namespace = "com.dmitriim.localaiplayground.ai.sherpa"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":ai:api"))
    implementation(project(":core:di"))
    // Microsoft ORT is packaged by :ai:chatterbox. Keep sherpa JNI, but remove its duplicate,
    // operator-minimized libonnxruntime.so so both engines use the same full ORT 1.27.0 binary.
    implementation(files(strippedSherpaAar))
    implementation(libs.kotlinx.coroutines.android)
}
