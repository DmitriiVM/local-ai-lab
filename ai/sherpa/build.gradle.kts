plugins {
    alias(libs.plugins.android.library)
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
    implementation(files("libs/sherpa-onnx-1.13.4.aar"))
}
