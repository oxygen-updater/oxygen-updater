import java.io.FileInputStream
import java.util.*

plugins {
    id(BuildPlugins.ANDROID_APPLICATION)
    id(BuildPlugins.FABRIC)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KOTLIN_ANDROID_EXTENSIONS)
    id(BuildPlugins.KOTLIN_KAPT)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    compileSdkVersion(AndroidSdk.COMPILE)
    buildToolsVersion = "29.0.2"

    defaultConfig {
        applicationId = "com.arjanvlek.oxygenupdater"

        minSdkVersion(AndroidSdk.MIN)
        targetSdkVersion(AndroidSdk.TARGET)

        versionCode = 62
        versionName = "3.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }

    packagingOptions {
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/LICENSE")
        exclude("META-INF/NOTICE")
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        // Config for releases and testing on a real device
        // Uses the production server, and reads system properties using the OnePlus/OxygenOS specific build.prop values
        getByName("release") {
            buildConfigField("String", "SERVER_BASE_URL", "\"https://oxygenupdater.com/api/v2.4/\"")
            buildConfigField("String", "FAQ_SERVER_URL", "\"https://oxygenupdater.com/inappfaq\"")
            buildConfigField("String", "NOTIFICATIONS_PREFIX", "\"\"")
            buildConfigField("String", "DEVICE_NAME_LOOKUP_KEY", "\"ro.display.series, ro.build.product\"")
            buildConfigField("String", "OS_VERSION_NUMBER_LOOKUP_KEY", "\"ro.rom.version, ro.oxygen.version, ro.build.ota.versionname\"")
            buildConfigField("String", "OS_OTA_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.ota\"")
            buildConfigField("String", "BUILD_FINGERPRINT_LOOKUP_KEY", "\"ro.build.oemfingerprint, ro.build.fingerprint\"")
            // Latter one is only used on very old OOS versions
            buildConfigField("String", "AB_UPDATE_LOOKUP_KEY", "\"ro.build.ab_update\"")
            buildConfigField("String", "SUPPORTED_BUILD_FINGERPRINT_KEYS", "\"release-keys\"")
            // Only devices using a properly signed (a.k.a. official) version of OxygenOS are supported
            buildConfigField("Boolean", "ADS_ARE_SUPPORTED", "true")

            signingConfig = signingConfigs.getByName("debug")

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            // to distinguish in app drawer and allow multiple builds to exist in parallel on the same device
            manifestPlaceholders = mapOf(
                "appName" to "Oxygen Updater",
                "fullPackageName" to defaultConfig.applicationId
            )
        }
        // Config for use during debugging and testing on an emulator
        // Uses the test server, and reads system properties using the default build.prop values present on any Android device/emulator
        getByName("debug") {
            buildConfigField("String", "SERVER_BASE_URL", "\"https://test.oxygenupdater.com/api/v2.4/\"")
            buildConfigField("String", "FAQ_SERVER_URL", "\"https://test.oxygenupdater.com/inappfaq\"")
            buildConfigField("String", "NOTIFICATIONS_PREFIX", "\"test_\"")
            buildConfigField("String", "DEVICE_NAME_LOOKUP_KEY", "\"ro.product.name\"")
            buildConfigField("String", "OS_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.release\"")
            buildConfigField("String", "OS_OTA_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.incremental\"")
            buildConfigField("String", "BUILD_FINGERPRINT_LOOKUP_KEY", "\"ro.build.fingerprint\"")
            buildConfigField("String", "AB_UPDATE_LOOKUP_KEY", "\"ro.build.ab_update\"")
            buildConfigField("String", "SUPPORTED_BUILD_FINGERPRINT_KEYS", "\"\"")
            // During debugging, all devices are supported
            buildConfigField("Boolean", "ADS_ARE_SUPPORTED", "true") // Ads also need to be tested

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = true

            // to distinguish in app drawer and allow multiple builds to exist in parallel on the same device
            versionNameSuffix = "-debug"
            applicationIdSuffix = ".debug"
            manifestPlaceholders = mapOf(
                "appName" to "Oxygen Updater (debug)",
                "fullPackageName" to defaultConfig.applicationId + ".debug"
            )
        }
        // Config for use during debugging locally on an emulator
        // Uses localhost at port 8000, and reads system properties using the default build.prop values present on any Android device/emulator
        create("localDebug") {
            buildConfigField("String", "SERVER_BASE_URL", "\"http://10.0.2.2:8000/api/v2.4/\"")
            buildConfigField("String", "FAQ_SERVER_URL", "\"http://10.0.2.2:8000/inappfaq\"")
            buildConfigField("String", "NOTIFICATIONS_PREFIX", "\"test_\"")
            buildConfigField("String", "DEVICE_NAME_LOOKUP_KEY", "\"ro.product.name\"")
            buildConfigField("String", "OS_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.release\"")
            buildConfigField("String", "OS_OTA_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.incremental\"")
            buildConfigField("String", "BUILD_FINGERPRINT_LOOKUP_KEY", "\"ro.build.fingerprint\"")
            buildConfigField("String", "AB_UPDATE_LOOKUP_KEY", "\"ro.build.ab_update\"")
            buildConfigField("String", "SUPPORTED_BUILD_FINGERPRINT_KEYS", "\"\"")
            // During debugging, all devices are supported
            buildConfigField("Boolean", "ADS_ARE_SUPPORTED", "true") // Ads also need to be tested

            isDebuggable = true

            // to distinguish in app drawer and allow multiple builds to exist in parallel on the same device
            versionNameSuffix = "-localDebug"
            applicationIdSuffix = ".localDebug"
            manifestPlaceholders = mapOf(
                "appName" to "Oxygen Updater (localDebug)",
                "fullPackageName" to defaultConfig.applicationId + ".localDebug"
            )
        }
        // Config for use during screenshot taking
        // Uses the real server, disables ads and reads system properties using the default build.prop values present on any Android device/emulator
        create("screenshot") {
            buildConfigField("String", "SERVER_BASE_URL", "\"https://oxygenupdater.com/api/v2.4/\"")
            buildConfigField("String", "FAQ_SERVER_URL", "\"https://oxygenupdater.com/inappfaq\"")
            buildConfigField("String", "NOTIFICATIONS_PREFIX", "\"\"")
            buildConfigField("String", "DEVICE_NAME_LOOKUP_KEY", "\"ro.product.name\"")
            buildConfigField("String", "OS_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.release\"")
            buildConfigField("String", "OS_OTA_VERSION_NUMBER_LOOKUP_KEY", "\"ro.build.version.incremental\"")
            buildConfigField("String", "BUILD_FINGERPRINT_LOOKUP_KEY", "\"ro.build.fingerprint\"")
            buildConfigField("String", "AB_UPDATE_LOOKUP_KEY", "\"ro.build.ab_update\"")
            buildConfigField("String", "SUPPORTED_BUILD_FINGERPRINT_KEYS", "\"\"")
            // During taking screenshots, all devices are supported
            buildConfigField("Boolean", "ADS_ARE_SUPPORTED", "false") // On screenshots no ads!

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = true

            // to distinguish in app drawer and allow multiple builds to exist in parallel on the same device
            applicationIdSuffix = ".screenshot"
            versionNameSuffix = "-screenshot"
            manifestPlaceholders = mapOf(
                "appName" to "Oxygen Updater (screenshot)",
                "fullPackageName" to defaultConfig.applicationId + ".screenshot"
            )
        }
        // Config for use on test devices. Uses the test server, and reads system properties using the LineageOS values from build.prop
        create("device") {
            buildConfigField("String", "SERVER_BASE_URL", "\"https://test.oxygenupdater.com/api/v2.4/\"")
            buildConfigField("String", "FAQ_SERVER_URL", "\"https://test.oxygenupdater.com/inappfaq\"")
            buildConfigField("String", "NOTIFICATIONS_PREFIX", "\"test_\"")
            buildConfigField("String", "DEVICE_NAME_LOOKUP_KEY", "\"ro.cm.device\"")
            buildConfigField("String", "OS_VERSION_NUMBER_LOOKUP_KEY", "\"ro.cm.display.version\"")
            buildConfigField("String", "OS_OTA_VERSION_NUMBER_LOOKUP_KEY", "\"ro.cm.display.version\"")
            buildConfigField("String", "BUILD_FINGERPRINT_LOOKUP_KEY", "\"ro.build.fingerprint\"")
            buildConfigField("String", "AB_UPDATE_LOOKUP_KEY", "\"ro.build.ab_update\"")
            buildConfigField("String", "SUPPORTED_BUILD_FINGERPRINT_KEYS", "\"\"")
            // On test device all build types are supported
            buildConfigField("Boolean", "ADS_ARE_SUPPORTED", "true") // On test device use ads!

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = true

            // to distinguish in app drawer and allow multiple builds to exist in parallel on the same device
            versionNameSuffix = "-device"
            applicationIdSuffix = ".device"
            manifestPlaceholders = mapOf(
                "appName" to "Oxygen Updater (device)",
                "fullPackageName" to defaultConfig.applicationId + ".device"
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

    implementation(Libraries.KOTLIN_STDLIB_JDK8)
    implementation(Libraries.KOTLIN_COROUTINES_CORE)
    implementation(Libraries.KOTLIN_COROUTINES_ANDROID)

    implementation(AndroidXLibraries.APP_COMPAT)
    implementation(AndroidXLibraries.BROWSER)
    implementation(AndroidXLibraries.CONSTRAINT_LAYOUT)
    implementation(AndroidXLibraries.RECYCLER_VIEW)

    implementation(AndroidXLibraries.KTX_CORE)
    implementation(AndroidXLibraries.KTX_FRAGMENT)
    implementation(AndroidXLibraries.KTX_LIFECYCLE_LIVEDATA)
    implementation(AndroidXLibraries.KTX_LIFECYCLE_VIEWMODEL)
    implementation(AndroidXLibraries.KTX_PREFERENCE)

    implementation(Libraries.MATERIAL)

    implementation(Libraries.CRASHLYTICS)
    implementation(Libraries.FIREBASE_ADS)
    implementation(Libraries.FIREBASE_MESSAGING)
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

    implementation(Libraries.JODA_CONVERT)
    implementation(Libraries.JODA_TIME)

    implementation(Libraries.PR_DOWNLOADER)

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
