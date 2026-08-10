package com.dmitriim.localaiplayground.buildlogic.quality

import org.gradle.api.Plugin
import org.gradle.api.Project

class StaticAnalysisConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        check(this == rootProject) {
            "local-ai.static-analysis must be applied to the root project."
        }

        pluginManager.apply("org.jlleitschuh.gradle.ktlint")

        tasks.register("staticAnalysis") {
            group = "verification"
            description = "Runs Android Lint where applicable, Detekt, and Ktlint across the project."

            dependsOn("ktlintCheck")
            allprojects
                .filter { project -> project != rootProject && project.buildFile.isFile }
                .forEach { project ->
                    dependsOn("${project.path}:detekt", "${project.path}:ktlintCheck")
                    project.pluginManager.withPlugin("com.android.application") {
                        dependsOn("${project.path}:lint")
                    }
                    project.pluginManager.withPlugin("com.android.library") {
                        dependsOn("${project.path}:lint")
                    }
                }

            val buildLogic = gradle.includedBuild("build-logic")
            dependsOn(
                buildLogic.task(":detekt"),
                buildLogic.task(":ktlintCheck"),
            )
        }

        Unit
    }
}
