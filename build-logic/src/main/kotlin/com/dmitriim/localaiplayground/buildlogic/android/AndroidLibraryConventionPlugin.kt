package com.dmitriim.localaiplayground.buildlogic.android

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("local-ai.android-static-analysis")

        extensions.configure<LibraryExtension> {
            compileSdk {
                version = release(AndroidSdkVersions.COMPILE_SDK_MAJOR) {
                    minorApiLevel = AndroidSdkVersions.COMPILE_SDK_MINOR
                }
            }
            defaultConfig {
                minSdk = AndroidSdkVersions.MIN_SDK
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }
}
