pluginManagement {
    repositories {
        google {
            // https://maven.google.com/web/index.html
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx?\\..*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            content {
                // for com.github.topjohnwu.libsu
                includeGroupByRegex("com\\.github\\..*")
            }
        }
    }
}

include(":app")
rootProject.name = "Oxygen Updater"
