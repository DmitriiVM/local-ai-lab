package com.dmitriim.localailab.buildlogic.quality

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidStaticAnalysisConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("local-ai.android-lint")
        pluginManager.apply("local-ai.detekt")
        pluginManager.apply("local-ai.ktlint")
    }
}
