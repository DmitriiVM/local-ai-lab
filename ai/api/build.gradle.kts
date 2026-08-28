plugins {
    id("local-ai.jvm.library")
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
}
