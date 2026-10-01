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
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Chageun"
include(":app")
include(":core:common")
include(":core:database")
include(":core:designsystem")
include(":core:domain")
include(":core:model")
include(":core:notification")
include(":core:security")
include(":core:testing")
include(":core:ui")
include(":data:history")
include(":data:maintenance")
include(":data:vehicle")
include(":feature:history")
include(":feature:home")
include(":feature:manage")
include(":feature:onboarding")
include(":feature:vehicle")
