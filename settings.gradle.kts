pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // for com.github.topjohnwu.libsu
    }
}

include(":app")
rootProject.name = "Oxygen Updater"
