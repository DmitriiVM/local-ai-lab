pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "LocalAiPlayground"
include(":app")
include(":ai:api")
include(":ai:chatterbox")
include(":ai:llamacpp")
include(":ai:litertlm")
include(":ai:sherpa")
include(":ai:vosk")
include(":ai:system")
include(":core:audio")
include(":core:di")
include(":core:model")
include(":core:navigation")
include(":core:result")
include(":core:ui")
include(":core:voice")
include(":source:database")
include(":source:models")
include(":source:runs")
include(":source:settings")
include(":feature:assistant")
include(":feature:device")
include(":feature:models")
include(":feature:playground")
include(":feature:runs")
include(":feature:settings")
include(":feature:stt")
include(":feature:tts")
