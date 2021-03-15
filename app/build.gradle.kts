import java.io.FileInputStream
import java.util.*

plugins {
    id(BuildPlugins.ANDROID_APPLICATION)
    id(BuildPlugins.FIREBASE_CRASHLYTICS)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KOTLIN_ANDROID_EXTENSIONS)
    id(BuildPlugins.KOTLIN_KAPT)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    compileSdkVersion(AndroidSdk.COMPILE)
    buildToolsVersion = AndroidSdk.BUILD_TOOLS

    defaultConfig {
        applicationId = "com.arjanvlek.oxygenupdater"

        minSdkVersion(AndroidSdk.MIN)
        targetSdkVersion(AndroidSdk.TARGET)

        versionCode = 77
        versionName = "5.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                // Add Room-specific arguments
                // https://developer.android.com/jetpack/androidx/releases/room#compiler-options
                arguments(
                    mapOf(
                        "room.schemaLocation" to "$projectDir/schemas",
                        "room.incremental" to "true",
                        "room.expandProjection" to "true"
                    )
                )
            }
        }

        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
            "proguard-rules-glide.pro"
        )
    }

    bundle {
        language {
            // Because the app has an in-app language switch feature, we need
            // to disable splitting configuration APKs for language resources.
            // This ensures that the app won't crash if the user selects a
            // language that isn't in their device language list.
            // This'll obviously increase APK size significantly.
            enableSplit = false
        }
    }

    packagingOptions {
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/LICENSE")
        exclude("META-INF/NOTICE")
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String?
        }
    }

    buildTypes {
        // Config for releases and testing on a real device
        // Uses the production server, and reads system properties using the OnePlus/OxygenOS specific build.prop values
        getByName("release") {
            buildConfigField("String", "SERVER_BASE_URL", "\"https://oxygenupdater.com/api/v2.6/\"")
            buildConfigField("String", "NOTIFICATIONS_PREFIX", "\"\"")
            buildConfigField("String", "DEVICE_NAME_LOOKUP_KEY", "\"ro.display.series, ro.build.product\"")
            buildConfigField("String", "OS_VERSION_NUMBER_LOOKUP_KEY", "\"ro.rom.version, ro.oxygen.version, ro.build.ota.versionname\"")
            buildConfigField("String", "OS_OTA_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.ota\"")
            buildConfigField("String", "BUILD_FINGERPRINT_LOOKUP_KEY", "\"ro.build.oemfingerprint, ro.build.fingerprint\"")
            // Latter one is only used on very old OOS versions
            buildConfigField("String", "AB_UPDATE_LOOKUP_KEY", "\"ro.build.ab_update\"")
            buildConfigField("String", "SUPPORTED_BUILD_FINGERPRINT_KEYS", "\"release-keys\"")

            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
        }
        // Config for use during debugging and testing on an emulator
        // Uses the test server, and reads system properties using the default build.prop values present on any Android device/emulator
        getByName("debug") {
            buildConfigField("String", "SERVER_BASE_URL", "\"https://test.oxygenupdater.com/api/v2.6/\"")
            buildConfigField("String", "NOTIFICATIONS_PREFIX", "\"test_\"")
            buildConfigField("String", "DEVICE_NAME_LOOKUP_KEY", "\"ro.product.name\"")
            buildConfigField("String", "OS_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.release\"")
            buildConfigField("String", "OS_OTA_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.incremental\"")
            buildConfigField("String", "BUILD_FINGERPRINT_LOOKUP_KEY", "\"ro.build.fingerprint\"")
            buildConfigField("String", "AB_UPDATE_LOOKUP_KEY", "\"ro.build.ab_update\"")
            buildConfigField("String", "SUPPORTED_BUILD_FINGERPRINT_KEYS", "\"\"")

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = true

            firebaseCrashlytics {
                mappingFileUploadEnabled = false
            }
        }
        // Config for use during debugging locally on an emulator
        // Uses localhost at port 8000, and reads system properties using the default build.prop values present on any Android device/emulator
        create("localDebug") {
            buildConfigField("String", "SERVER_BASE_URL", "\"http://10.0.2.2:8000/api/v2.6/\"")
            buildConfigField("String", "NOTIFICATIONS_PREFIX", "\"test_\"")
            buildConfigField("String", "DEVICE_NAME_LOOKUP_KEY", "\"ro.product.name\"")
            buildConfigField("String", "OS_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.release\"")
            buildConfigField("String", "OS_OTA_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.incremental\"")
            buildConfigField("String", "BUILD_FINGERPRINT_LOOKUP_KEY", "\"ro.build.fingerprint\"")
            buildConfigField("String", "AB_UPDATE_LOOKUP_KEY", "\"ro.build.ab_update\"")
            buildConfigField("String", "SUPPORTED_BUILD_FINGERPRINT_KEYS", "\"\"")

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = true

            firebaseCrashlytics {
                mappingFileUploadEnabled = false
            }
        }

        val languages = fileTree("src/main/res") {
            include("values-*/strings.xml")
        }.files.map { file ->
            file.parentFile.name.replace(
                "values-",
                ""
            )
        }.joinToString { str ->
            "\"$str\""
        }

        // to distinguish in app drawer and allow multiple builds to exist in parallel on the same device
        buildTypes.forEach {
            it.buildConfigField(
                "String[]",
                "SUPPORTED_LANGUAGES",
                "{\"en\", $languages}"
            )
            if (it.name != "release") {
                it.versionNameSuffix = "-${it.name}"
                it.applicationIdSuffix = ".${it.name}"
                it.resValue("string", "app_name", "Oxygen Updater (${it.name})")
                it.addManifestPlaceholders(
                    mapOf(
                        "hostName" to "test.oxygenupdater.com"
                    )
                )
            } else {
                it.resValue("string", "app_name", "Oxygen Updater")
                it.addManifestPlaceholders(
                    mapOf(
                        "hostName" to "oxygenupdater.com"
                    )
                )
            }

            it.addManifestPlaceholders(
                mapOf(
                    "shortcutXml" to "@xml/shortcuts_${it.name.toLowerCase()}"
                )
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    testBuildType = "debug"
}

buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(Libraries.KOTLIN_COROUTINES_CORE)
    implementation(Libraries.KOTLIN_COROUTINES_ANDROID)

    implementation(AndroidXLibraries.APP_COMPAT)
    implementation(AndroidXLibraries.BROWSER)
    implementation(AndroidXLibraries.CONSTRAINT_LAYOUT)
    implementation(AndroidXLibraries.RECYCLER_VIEW)
    implementation(AndroidXLibraries.ROOM_KTX)
    implementation(AndroidXLibraries.ROOM_RUNTIME)
    kapt(AndroidXLibraries.ROOM_COMPILER)

    implementation(AndroidXLibraries.KTX_CORE)
    implementation(AndroidXLibraries.KTX_FRAGMENT)
    implementation(AndroidXLibraries.KTX_LIFECYCLE_LIVEDATA)
    implementation(AndroidXLibraries.KTX_LIFECYCLE_VIEWMODEL)
    implementation(AndroidXLibraries.KTX_PREFERENCE)
    implementation(AndroidXLibraries.KTX_WORK)

    implementation(Libraries.MATERIAL)

    implementation(platform(Libraries.FIREBASE_BOM))
    implementation(Libraries.FIREBASE_ADS)
    implementation(Libraries.FIREBASE_ANALYTICS_KTX)
    implementation(Libraries.FIREBASE_CRASHLYTICS_KTX)
    implementation(Libraries.FIREBASE_MESSAGING_KTX)

    implementation(Libraries.GOOGLE_PLAY_BILLING)
    implementation(Libraries.GOOGLE_PLAY_BILLING_KTX)
    implementation(Libraries.PLAY_CORE)
    implementation(Libraries.PLAY_SERVICES_BASE)

    implementation(Libraries.KOIN)
    implementation(Libraries.KOIN_FRAGMENT)
    implementation(Libraries.KOIN_SCOPE)
    implementation(Libraries.KOIN_VIEWMODEL)

    implementation(Libraries.OKHTTP_LOGGING_INTERCEPTOR)
    implementation(Libraries.RETROFIT)
    implementation(Libraries.RETROFIT_CONVERTER_JACKSON)

    implementation(Libraries.JACKSON_KOTLIN_MODULE)

    implementation(Libraries.GLIDE)
    kapt(Libraries.GLIDE_COMPILER)

    implementation(Libraries.FACEBOOK_SHIMMER)

    implementation(Libraries.THREE_TEN_ABP)

    implementation(Libraries.CHAINFIRE_LIBSUPERUSER)

    implementation(Libraries.A_FILE_CHOOSER)

    testImplementation(TestLibraries.JUNIT4)
    testImplementation(TestLibraries.KOTLIN_TEST_JUNIT)
    testImplementation(TestLibraries.KOIN_TEST)

    androidTestImplementation(TestLibraries.ANNOTATION)
    androidTestImplementation(TestLibraries.ESPRESSO_CORE)
    androidTestImplementation(TestLibraries.JUNIT_EXT)
    androidTestImplementation(TestLibraries.RULES)
    androidTestImplementation(TestLibraries.RUNNER)
}

apply(plugin = BuildPlugins.GOOGLE_SERVICES)
