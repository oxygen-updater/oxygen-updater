// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.gms.services) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
}

configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
        failOnNonReproducibleResolution()
    }
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
