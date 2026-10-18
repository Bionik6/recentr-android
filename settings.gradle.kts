pluginManagement {
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Recentr"

include(":app")
include(":core:designsystem")
include(":core:model")
include(":core:data")
include(":feature:onboarding")
include(":feature:home")
include(":feature:rules")
include(":feature:profile")
include(":feature:focus")
include(":feature:insights")
