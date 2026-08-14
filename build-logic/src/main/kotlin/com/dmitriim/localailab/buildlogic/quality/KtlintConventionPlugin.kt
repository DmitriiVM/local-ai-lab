package com.dmitriim.localailab.buildlogic.quality

import org.gradle.api.Plugin
import org.gradle.api.Project

class KtlintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    }
}
