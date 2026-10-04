import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing.
//
// dterm ships a release key committed in the repo so that the GitHub workflow — and
// every other machine that clones it — signs every release build with the SAME key.
// That is what makes a release APK installable/updatable over an existing dterm
// install instead of failing with a signature mismatch. This key is for
// testing/distribution only; a real Play-upload key must still be configured for
// Play. It is NOT a secret: the same values are documented in
// keystore.properties.example.
//
// keystore.properties is machine-local (gitignored) and may point at a keystore
// OUTSIDE the repo. When neither it nor the committed key is usable, the release
// build stays unsigned rather than failing the whole build.
val releaseKeyStoreFile = file("keystore/dterm-release.keystore")
val keystorePropertiesFile = file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.qali.dterm"
    compileSdk = 36
    ndkVersion = "29.0.14206865"

    defaultConfig {
        // dterm's own Android package name. It is a completely separate app from
        // Termux: own uid, own data dir (/data/data/com.qali.dterm), own
        // permissions, no com.termux permission and no sharedUserId. That is what
        // lets both apps be installed side by side on one device.
        applicationId = "com.qali.dterm"
        minSdk = 26
        targetSdk = 36
        versionCode = 27
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // dterm branding built into the APK; res/values/strings.xml is English-only.
        buildConfigField("String", "APP_NAME", "\"dterm\"")
        buildConfigField("String", "APPLICATION_ID", "\"com.qali.dterm\"")
        // Reported to the bundled host script during `bootstrap`. Kept in sync with
        // VERSION in app/src/main/assets/dterm-host.sh.
        buildConfigField("String", "HOST_SCRIPT_VERSION", "\"1.2.0\"")
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        // Pin the debug signing key to a keystore committed in the repo so EVERY
        // build (any machine, regardless of ANDROID_USER_HOME/ANDROID_SDK_HOME)
        // signs with the SAME key. Without this, the debug key is resolved from
        // ~/.android or ~/.config/.android depending on the environment; when that
        // path changed, the new APK's signature no longer matched the one already
        // installed on devices and over-install failed with "app not installed".
        // A debug keystore holds no secret (standard android/androiddebugkey
        // credentials), so committing it is safe.
        getByName("debug") {
            storeFile = file("keystore/ldfa-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Deterministic release signing: the committed key first, a machine-local
        // keystore.properties second, and no signing config at all as the fallback.
        if (releaseKeyStoreFile.exists()) {
            create("release") {
                storeFile = releaseKeyStoreFile
                storePassword = "dtermtestkey1234"
                keyAlias = "dtermreleasekey"
                keyPassword = "dtermtestkey1234"
            }
        } else if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
            // Ship both the ARM64 (real device) and x86_64 (emulator/verification)
            // runtimes, each with its own prefix bootstrap. Every native library,
            // the rootfs bootstrap and the host scripts are bundled into this APK.
            ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs.useLegacyPackaging = true
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        resources.excludes += "/META-INF/DEPENDENCIES"
    }

    lint {
        disable += "ExpiredTargetSdkVersion"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")

    implementation(project(":termux-runtime"))
    implementation(project(":embedded-x11"))
    // Direct dep so the app can register its proot exec-rewriter into
    // com.termux.shared.shell.ExecInterceptor (Play/targetSdk-35 W^X path).
    implementation(project(":termux-shared"))

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    // Android ships org.json only as a stub for unit tests (every method throws),
    // so pull in the real implementation for JVM tests. Not shipped in the APK,
    // which uses the platform org.json at runtime.
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}