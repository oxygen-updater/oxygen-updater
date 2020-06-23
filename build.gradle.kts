// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath(BuildPlugins.ANDROID_GRADLE_PLUGIN)
        classpath(BuildPlugins.GOOGLE_SERVICES_PLUGIN)
        classpath(BuildPlugins.FIREBASE_GRADLE_PLUGIN)
        classpath(BuildPlugins.KOTLIN_GRADLE_PLUGIN)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.register("clean").configure {
    delete("build")
}
