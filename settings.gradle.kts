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

rootProject.name = "BetterType"
include(":app")
include(":lt:morfologik-speller", ":lt:core", ":lt:lang-en", ":lt:lang-de", ":lt:lang-fr", ":lt:lang-es", ":lt:hunspell-android")
 
