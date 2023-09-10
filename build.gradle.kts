// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // https://developer.android.com/studio/releases/gradle-plugin
    // https://mvnrepository.com/artifact/com.android.tools.build/gradle?repo=google
    id("com.android.application") version "8.1.1" apply false

    // https://developers.google.com/android/guides/releases#:~:text=com.google.gms%3Agoogle%2Dservices
    id("com.google.gms.google-services") version "4.3.15" apply false

    // https://firebase.google.com/support/release-notes/android
    id("com.google.firebase.crashlytics") version "2.9.9" apply false

    id("com.google.devtools.ksp") version KSP_VERSION apply false

    id("org.jetbrains.kotlin.android") version KOTLIN_VERSION apply false
}

tasks.register("clean").configure {
    delete("build")
}

// https://docs.gradle.org/current/userguide/performance.html#execute_tests_in_parallel
tasks.withType<Test>().configureEach {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

// https://docs.gradle.org/current/userguide/performance.html#run_the_compiler_as_a_separate_process
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
}
