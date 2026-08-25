package com.dmitriim.localailab.buildlogic.android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Applies the ABI set shared by modules that package local native runtimes. */
class NativeRuntimeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> {
                defaultConfig {
                    ndk {
                        abiFilters.addAll(SUPPORTED_NATIVE_ABIS)
                    }
                }
            }
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> {
                defaultConfig {
                    ndk {
                        abiFilters.addAll(SUPPORTED_NATIVE_ABIS)
                    }
                }
            }
        }
    }

    private companion object {
        val SUPPORTED_NATIVE_ABIS = setOf("arm64-v8a", "x86_64")
    }
}
