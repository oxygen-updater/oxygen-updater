// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        // https://developer.android.com/studio/releases/gradle-plugin
        classpath("com.android.tools.build:gradle:8.1.1")

        // https://developers.google.com/android/guides/releases#:~:text=com.google.gms%3Agoogle%2Dservices
        classpath("com.google.gms:google-services:4.3.15")

        // https://firebase.google.com/support/release-notes/android
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.8")

        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean").configure {
    delete("build")
}
