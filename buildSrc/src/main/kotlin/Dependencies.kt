// https://kotlinlang.org/docs/releases.html#release-details
// https://github.com/JetBrains/kotlin/blob/master/ChangeLog.md
// https://blog.jetbrains.com/kotlin/category/releases/
const val KOTLIN_VERSION = "1.9.0"

// https://github.com/google/ksp/releases
const val KSP_VERSION = "1.9.0-1.0.12" // keep in sync with Kotlin version

// https://developer.android.com/jetpack/androidx/releases/compose-kotlin
// https://developer.android.com/jetpack/androidx/releases/compose-compiler
const val COMPOSE_COMPILER = "1.5.1" // keep in sync with Kotlin version

// https://mvnrepository.com/artifact/io.insert-koin/koin-android
// https://github.com/InsertKoinIO/koin/tags
// https://github.com/InsertKoinIO/koin/blob/master/CHANGELOG.md
const val KOIN_VERSION = "3.4.2"

object BuildPlugins {
    private object Versions {
        // https://developer.android.com/studio/releases/gradle-plugin
        // https://mvnrepository.com/artifact/com.android.tools.build/gradle?repo=google
        const val BUILD_GRADLE_PLUGIN = "8.1.0"

        // https://developers.google.com/android/guides/releases#:~:text=com.google.gms%3Agoogle%2Dservices%3A
        const val GOOGLE_SERVICES_PLUGIN = "4.3.15"

        // https://firebase.google.com/support/release-notes/android
        const val FIREBASE_GRADLE_PLUGIN = "2.9.5"
    }

    // https://developer.android.com/studio/releases/gradle-plugin
    const val ANDROID_GRADLE_PLUGIN = "com.android.tools.build:gradle:${Versions.BUILD_GRADLE_PLUGIN}"

    // https://developers.google.com/android/guides/releases#:~:text=com.google.gms%3Agoogle%2Dservices
    const val GOOGLE_SERVICES_PLUGIN = "com.google.gms:google-services:${Versions.GOOGLE_SERVICES_PLUGIN}"

    // https://firebase.google.com/support/release-notes/android
    const val FIREBASE_GRADLE_PLUGIN = "com.google.firebase:firebase-crashlytics-gradle:${Versions.FIREBASE_GRADLE_PLUGIN}"
    const val KOTLIN_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION"

    const val ANDROID_APPLICATION = "com.android.application"
    const val GOOGLE_SERVICES = "com.google.gms.google-services"
    const val FIREBASE_CRASHLYTICS = "com.google.firebase.crashlytics"
    const val KSP = "com.google.devtools.ksp"
}

object AndroidSdk {
    // https://developer.android.com/studio/releases/build-tools
    const val BUILD_TOOLS = "34.0.0"
    const val MIN = 21
    const val COMPILE = 34
    const val TARGET = 31 // TODO: 31st August 2023 onwards, app updates require targeting 33
}

// AndroidX: https://developer.android.com/jetpack/androidx/releases
object AndroidXLibraries {
    private object Versions {
        // https://developer.android.com/jetpack/androidx/releases/activity
        const val ACTIVITY = "1.7.2"

        // https://developer.android.com/jetpack/androidx/releases/annotation
        const val ANNOTATION = "1.6.0"

        // https://developer.android.com/jetpack/androidx/releases/appcompat
        const val APP_COMPAT = "1.6.1"

        // https://developer.android.com/jetpack/androidx/releases/browser
        const val BROWSER = "1.5.0"

        // https://developer.android.com/jetpack/androidx/releases/compose#versions
        const val COMPOSE = "1.5.0"

        // https://developer.android.com/jetpack/androidx/releases/compose#versions
        // TODO(compose): remove DefaultTextStyle from Typography once M2 1.6.0 or M3 1.2.0 go stable; will be default then
        const val COMPOSE_MATERIAL = "1.5.0"

        // https://developer.android.com/jetpack/androidx/releases/compose-material3
        const val COMPOSE_MATERIAL3 = "1.1.1"

        // https://developer.android.com/jetpack/androidx/releases/constraintlayout
        const val CONSTRAINT_LAYOUT = "2.1.4"

        // https://developer.android.com/jetpack/androidx/releases/core
        const val CORE_KTX = "1.10.1"

        // https://developer.android.com/jetpack/androidx/releases/core
        const val CORE_SPLASHSCREEN = "1.0.1"

        // https://developer.android.com/jetpack/androidx/releases/swiperefreshlayout
        const val SWIPE_REFRESH_LAYOUT = "1.1.0"

        // https://developer.android.com/jetpack/androidx/releases/fragment
        const val KTX_FRAGMENT = "1.5.7"

        // https://developer.android.com/jetpack/androidx/releases/preference
        const val KTX_PREFERENCE = "1.2.0"

        // https://developer.android.com/jetpack/androidx/releases/work
        const val KTX_WORK = "2.8.1"

        // https://developer.android.com/jetpack/androidx/releases/lifecycle
        const val LIFECYCLE = "2.6.1"

        // https://developer.android.com/jetpack/androidx/releases/navigation#versions
        const val NAVIGATION = "2.6.0"

        // https://developer.android.com/jetpack/androidx/releases/recyclerview
        const val RECYCLER_VIEW = "1.3.0"

        // https://developer.android.com/jetpack/androidx/releases/room
        const val ROOM = "2.5.1"
    }

    // https://developer.android.com/jetpack/androidx/releases/annotation
    const val ANNOTATION = "androidx.annotation:annotation:${Versions.ANNOTATION}"

    // https://developer.android.com/jetpack/androidx/releases/appcompat
    const val APP_COMPAT = "androidx.appcompat:appcompat:${Versions.APP_COMPAT}"

    // https://developer.android.com/jetpack/androidx/releases/browser
    const val BROWSER = "androidx.browser:browser:${Versions.BROWSER}"

    // https://developer.android.com/jetpack/androidx/releases/compose#versions
    const val COMPOSE_ANIMATION = "androidx.compose.animation:animation:${Versions.COMPOSE}"
    const val COMPOSE_ANIMATION_GRAPHICS = "androidx.compose.animation:animation-graphics:${Versions.COMPOSE}"
    const val COMPOSE_FOUNDATION = "androidx.compose.foundation:foundation:${Versions.COMPOSE_MATERIAL}"
    const val COMPOSE_MATERIAL = "androidx.compose.material:material:${Versions.COMPOSE_MATERIAL}"
    const val COMPOSE_MATERIAL3 = "androidx.compose.material3:material3:${Versions.COMPOSE_MATERIAL3}"
    const val COMPOSE_MATERIAL_ICONS_EXTENDED = "androidx.compose.material:material-icons-extended:${Versions.COMPOSE_MATERIAL}"
    const val COMPOSE_RUNTIME_LIVEDATA = "androidx.compose.runtime:runtime-livedata:${Versions.COMPOSE}"
    const val COMPOSE_UI = "androidx.compose.ui:ui:${Versions.COMPOSE}"
    const val COMPOSE_UI_TOOLING = "androidx.compose.ui:ui-tooling:${Versions.COMPOSE}"
    const val COMPOSE_UI_TOOLING_PREVIEW = "androidx.compose.ui:ui-tooling-preview:${Versions.COMPOSE}"

    // https://developer.android.com/jetpack/androidx/releases/activity
    const val ACTIVITY_COMPOSE = "androidx.activity:activity-compose:${Versions.ACTIVITY}"

    // https://developer.android.com/jetpack/androidx/releases/navigation#versions
    const val NAVIGATION_COMPOSE = "androidx.navigation:navigation-compose:${Versions.NAVIGATION}"

    // https://developer.android.com/jetpack/androidx/releases/constraintlayout
    const val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:${Versions.CONSTRAINT_LAYOUT}"

    // https://developer.android.com/jetpack/androidx/releases/core
    const val CORE_KTX = "androidx.core:core-ktx:${Versions.CORE_KTX}"
    const val CORE_SPLASHSCREEN = "androidx.core:core-splashscreen:${Versions.CORE_SPLASHSCREEN}"

    // https://developer.android.com/jetpack/androidx/releases/swiperefreshlayout
    const val SWIPE_REFRESH_LAYOUT = "androidx.swiperefreshlayout:swiperefreshlayout:${Versions.SWIPE_REFRESH_LAYOUT}"

    // https://developer.android.com/jetpack/androidx/releases/recyclerview
    const val RECYCLER_VIEW = "androidx.recyclerview:recyclerview:${Versions.RECYCLER_VIEW}"

    // https://developer.android.com/jetpack/androidx/releases/room
    const val ROOM_COMPILER = "androidx.room:room-compiler:${Versions.ROOM}"
    const val ROOM_KTX = "androidx.room:room-ktx:${Versions.ROOM}"
    const val ROOM_RUNTIME = "androidx.room:room-runtime:${Versions.ROOM}"

    // https://developer.android.com/jetpack/androidx/releases/fragment
    const val KTX_FRAGMENT = "androidx.fragment:fragment-ktx:${Versions.KTX_FRAGMENT}"

    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    const val LIFECYCLE_LIVEDATA_KTX = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.LIFECYCLE}"
    const val LIFECYCLE_VIEWMODEL_KTX = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.LIFECYCLE}"
    const val LIFECYCLE_COMPILER = "androidx.lifecycle:lifecycle-compiler:${Versions.LIFECYCLE}"
    const val LIFECYCLE_RUNTIME_COMPOSE = "androidx.lifecycle:lifecycle-runtime-compose:${Versions.LIFECYCLE}"

    // https://developer.android.com/jetpack/androidx/releases/preference
    const val KTX_PREFERENCE = "androidx.preference:preference-ktx:${Versions.KTX_PREFERENCE}"

    // https://developer.android.com/jetpack/androidx/releases/work
    const val KTX_WORK = "androidx.work:work-runtime-ktx:${Versions.KTX_WORK}"
}

object GoogleAccompanistLibraries {
    // https://github.com/google/accompanist#compose-versions
    // https://github.com/google/accompanist/releases
    private const val VERSION = "0.31.6-rc" // keep in sync with Compose version

    const val SYSTEM_UI_CONTROLLER = "com.google.accompanist:accompanist-systemuicontroller:$VERSION"
    const val PERMISSIONS = "com.google.accompanist:accompanist-permissions:$VERSION"
    const val PLACEHOLDER_MATERIAL = "com.google.accompanist:accompanist-placeholder-material:$VERSION"
    const val PLACEHOLDER_MATERIAL3 = "com.google.accompanist:accompanist-placeholder-material3:$VERSION"
    const val WEBVIEW = "com.google.accompanist:accompanist-webview:$VERSION"
}

object Libraries {
    private object Versions {
        // https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md
        const val ANDROID_TOOLS_DESUGAR = "2.0.3"

        // https://github.com/coil-kt/coil/blob/main/CHANGELOG.md
        const val COIL = "2.4.0"

        // https://github.com/facebook/shimmer-android/releases
        const val FACEBOOK_SHIMMER = "0.5.0"

        // https://firebase.google.com/support/release-notes/android
        const val FIREBASE_BOM = "32.1.0"

        // https://developer.android.com/google/play/billing/release-notes
        const val GOOGLE_PLAY_BILLING = "5.1.0"

        // Note: Before updating version, make sure this library's kotlin-reflect dependency's version is the same as kotlin-stdlib's version above
        // Note: Stay on 2.13.x, as 2.14.x doesn't support Android 7.1/Nougat and below (API < 26)
        // Check https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin/<version>
        const val JACKSON_KOTLIN_MODULE = "2.13.5"

        // https://github.com/Kotlin/kotlinx.coroutines/releases
        const val KOTLIN_COROUTINES = "1.7.1"

        // https://github.com/topjohnwu/libsu/blob/master/CHANGELOG.md
        const val LIBSU = "5.1.0"

        // https://github.com/material-components/material-components-android/releases
        const val MATERIAL = "1.9.0"

        // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
        // https://github.com/square/okhttp/blob/master/CHANGELOG.md
        // https://square.github.io/okhttp/changelog/
        const val OKHTTP_LOGGING_INTERCEPTOR = "4.11.0"

        // https://developer.android.com/reference/com/google/android/play/core/release-notes-in_app_updates
        const val PLAY_CORE_IN_APP_UPDATES = "2.1.0"

        // https://developers.google.com/android/guides/releases
        // https://developers.google.com/android/guides/releases#:~:text=com.google.android.gms%3Aplay%2Dservices%2Dbase%3A
        const val PLAY_SERVICES_BASE = "18.2.0"

        // https://developers.google.com/android/guides/releases
        // https://developers.google.com/admob/android/rel-notes
        const val PLAY_SERVICES_ADS = "22.1.0"

        // https://github.com/square/retrofit/tags
        // https://github.com/square/retrofit/blob/master/CHANGELOG.md
        const val RETROFIT = "2.9.0"
    }

    // https://github.com/Kotlin/kotlinx.coroutines/releases
    const val KOTLIN_COROUTINES_CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KOTLIN_COROUTINES}"
    const val KOTLIN_COROUTINES_ANDROID = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.KOTLIN_COROUTINES}"

    // Google Material Components for Android
    // https://github.com/material-components/material-components-android/releases
    const val MATERIAL = "com.google.android.material:material:${Versions.MATERIAL}"

    // Google Firebase
    // https://firebase.google.com/support/release-notes/android
    const val FIREBASE_BOM = "com.google.firebase:firebase-bom:${Versions.FIREBASE_BOM}"

    // Versions are managed by BOM
    const val FIREBASE_ANALYTICS_KTX = "com.google.firebase:firebase-analytics-ktx"
    const val FIREBASE_CRASHLYTICS_KTX = "com.google.firebase:firebase-crashlytics-ktx"
    const val FIREBASE_MESSAGING_KTX = "com.google.firebase:firebase-messaging-ktx"

    // Google Play stuff
    // https://developer.android.com/google/play/billing/release-notes
    const val GOOGLE_PLAY_BILLING = "com.android.billingclient:billing:${Versions.GOOGLE_PLAY_BILLING}"
    const val GOOGLE_PLAY_BILLING_KTX = "com.android.billingclient:billing-ktx:${Versions.GOOGLE_PLAY_BILLING}"

    // https://developer.android.com/reference/com/google/android/play/core/release-notes-in_app_updates
    const val PLAY_CORE_IN_APP_UPDATES = "com.google.android.play:app-update:${Versions.PLAY_CORE_IN_APP_UPDATES}"

    // https://developers.google.com/android/guides/releases
    const val PLAY_SERVICES_BASE = "com.google.android.gms:play-services-base:${Versions.PLAY_SERVICES_BASE}"
    const val PLAY_SERVICES_ADS = "com.google.android.gms:play-services-ads:${Versions.PLAY_SERVICES_ADS}"

    // Koin for Android
    // https://insert-koin.io/docs/setup/stable
    // https://github.com/InsertKoinIO/koin/releases
    // https://github.com/InsertKoinIO/koin/blob/master/CHANGELOG.md
    const val KOIN = "io.insert-koin:koin-android:${KOIN_VERSION}"

    // https://square.github.io/okhttp/changelog/
    const val OKHTTP_LOGGING_INTERCEPTOR = "com.squareup.okhttp3:logging-interceptor:${Versions.OKHTTP_LOGGING_INTERCEPTOR}"

    // https://github.com/square/retrofit/tags
    // https://github.com/square/retrofit/blob/master/CHANGELOG.md
    const val RETROFIT = "com.squareup.retrofit2:retrofit:${Versions.RETROFIT}"
    const val RETROFIT_CONVERTER_JACKSON = "com.squareup.retrofit2:converter-jackson:${Versions.RETROFIT}"

    // Note: Before updating version, make sure this library's kotlin-reflect dependency's version is the same as kotlin-stdlib's version above
    // Check https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin/<version>
    const val JACKSON_KOTLIN_MODULE = "com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.JACKSON_KOTLIN_MODULE}"

    // https://github.com/coil-kt/coil/blob/main/CHANGELOG.md
    const val COIL_COMPOSE = "io.coil-kt:coil-compose:${Versions.COIL}"

    // https://github.com/facebook/shimmer-android/releases
    const val FACEBOOK_SHIMMER = "com.facebook.shimmer:shimmer:${Versions.FACEBOOK_SHIMMER}"

    // https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md
    const val ANDROID_TOOLS_DESUGAR = "com.android.tools:desugar_jdk_libs:${Versions.ANDROID_TOOLS_DESUGAR}"

    // https://github.com/topjohnwu/libsu/blob/master/CHANGELOG.md
    const val LIBSU_CORE = "com.github.topjohnwu.libsu:core:${Versions.LIBSU}"
    const val LIBSU_NIO = "com.github.topjohnwu.libsu:nio:${Versions.LIBSU}"
    const val LIBSU_SERVICE = "com.github.topjohnwu.libsu:service:${Versions.LIBSU}"
}

object TestLibraries {
    private object Versions {
        // https://developer.android.com/jetpack/androidx/releases/annotation
        const val ANNOTATION = "1.6.0"

        // https://developer.android.com/jetpack/androidx/releases/test
        const val ESPRESSO = "3.5.1"
        const val JUNIT_EXT = "1.1.5"

        // https://github.com/junit-team/junit4/releases
        const val JUNIT4 = "4.13.2"

        // https://developer.android.com/jetpack/androidx/releases/test
        const val RULES = "1.5.0"
        const val RUNNER = "1.5.2"
    }

    // https://github.com/junit-team/junit4/releases
    const val JUNIT4 = "junit:junit:${Versions.JUNIT4}"

    // https://developer.android.com/jetpack/androidx/releases/test
    const val JUNIT_EXT = "androidx.test.ext:junit:${Versions.JUNIT_EXT}"

    const val KOTLIN_TEST_JUNIT = "org.jetbrains.kotlin:kotlin-test-junit:$KOTLIN_VERSION"

    // https://insert-koin.io/docs/setup/stable
    // https://github.com/InsertKoinIO/koin/releases
    // https://github.com/InsertKoinIO/koin/blob/master/CHANGELOG.md
    const val KOIN_TEST = "io.insert-koin:koin-test:${KOIN_VERSION}"

    // https://developer.android.com/jetpack/androidx/releases/annotation
    const val ANNOTATION = "androidx.annotation:annotation:${Versions.ANNOTATION}"

    // https://developer.android.com/jetpack/androidx/releases/test
    const val ESPRESSO_CORE = "androidx.test.espresso:espresso-core:${Versions.ESPRESSO}"
    const val RULES = "androidx.test:rules:${Versions.RULES}"
    const val RUNNER = "androidx.test:runner:${Versions.RUNNER}"
}
