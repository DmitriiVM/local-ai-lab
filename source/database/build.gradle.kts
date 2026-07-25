plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.dmitriim.localaiplayground.source.database"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":core:di"))
    implementation(project(":core:model"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.metro.runtime)
}
