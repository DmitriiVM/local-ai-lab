package com.dmitriim.localaiplayground.buildlogic.dependency

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

class FeatureBoundaryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        check(this == rootProject) {
            "local-ai.feature-boundaries must be applied to the root project."
        }

        val verifyFeatureBoundaries = tasks.register("verifyFeatureBoundaries") {
            group = "verification"
            description = "Verifies that feature modules do not depend on other feature modules."
        }

        gradle.projectsEvaluated {
            val violations = allprojects
                .filter { it.path.startsWith(FEATURE_PATH_PREFIX) }
                .flatMap { project ->
                    project.configurations.flatMap { configuration ->
                        configuration.dependencies
                            .withType(ProjectDependency::class.java)
                            .mapNotNull { dependency ->
                                dependency.path
                                    .takeIf { it.startsWith(FEATURE_PATH_PREFIX) && it != project.path }
                                    ?.let { targetPath -> "${project.path}:${configuration.name} -> $targetPath" }
                            }
                    }
                }
            if (violations.isNotEmpty()) {
                throw GradleException(
                    "Feature modules must not depend on other feature modules:\n${violations.joinToString("\n")}",
                )
            }
            allprojects
                .filter { it.path.startsWith(FEATURE_PATH_PREFIX) }
                .forEach { project ->
                    project.tasks.matching { it.name == "check" }
                        .configureEach { dependsOn(verifyFeatureBoundaries) }
                }
        }
    }

    private companion object {
        const val FEATURE_PATH_PREFIX = ":feature:"
    }
}
