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

rootProject.name = "LocalAiLab"
include(":app")
include(":ai:api")
include(":ai:performance")
include(":ai:runtime")
include(":ai:chatterbox")
include(":ai:llamacpp")
include(":ai:litertlm")
include(":ai:sherpa")
include(":ai:vosk")
include(":ai:system")
include(":core:audio")
include(":core:di")
include(":core:navigation")
include(":core:performance")
include(":core:operation")
include(":core:security")
include(":core:ui")
include(":feature:assistant:api")
include(":feature:assistant:impl")
include(":feature:benchmark:api")
include(":feature:benchmark:impl")
include(":feature:device:api")
include(":feature:device:impl")
include(":feature:models:api")
include(":feature:models:impl")
include(":feature:playground:api")
include(":feature:playground:impl")
include(":feature:runs:api")
include(":feature:runs:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
include(":feature:stt:api")
include(":feature:stt:impl")
include(":feature:tts:api")
include(":feature:tts:impl")
