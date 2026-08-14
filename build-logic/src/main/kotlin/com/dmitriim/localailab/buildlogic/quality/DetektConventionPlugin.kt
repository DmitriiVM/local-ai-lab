package com.dmitriim.localailab.buildlogic.quality

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.detekt")

        extensions.configure<DetektExtension> {
            basePath.set(rootProject.layout.projectDirectory)
            buildUponDefaultConfig.set(true)
            config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
            ignoreFailures.set(false)
            parallel.set(false)
        }

        tasks.withType<Detekt>().configureEach {
            exclude("**/build/**")
            exclude("**/third_party/**")
            reports {
                html.required.set(true)
                sarif.required.set(true)
            }
        }
    }
}
