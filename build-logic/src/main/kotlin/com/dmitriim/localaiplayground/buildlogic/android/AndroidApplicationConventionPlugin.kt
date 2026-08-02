package com.dmitriim.localaiplayground.buildlogic.android

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("local-ai.android-static-analysis")

        extensions.configure<ApplicationExtension> {
            compileSdk {
                version = release(AndroidSdkVersions.COMPILE_SDK_MAJOR) {
                    minorApiLevel = AndroidSdkVersions.COMPILE_SDK_MINOR
                }
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }
}
