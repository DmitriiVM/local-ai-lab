package com.dmitriim.localailab.buildlogic.android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> {
                configureLint()
            }
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> {
                configureLint()
            }
        }
    }
}

private fun ApplicationExtension.configureLint() {
    lint {
        abortOnError = true
        checkDependencies = true
        checkReleaseBuilds = true
        htmlReport = true
        sarifReport = true
    }
}

private fun LibraryExtension.configureLint() {
    lint {
        abortOnError = true
        checkDependencies = true
        checkReleaseBuilds = true
        htmlReport = true
        sarifReport = true
    }
}
