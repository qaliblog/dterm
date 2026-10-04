pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // The Android Gradle Plugin's plugin-marker artifacts are not published to
    // every repository the plugin portal mirrors, so `plugins { id("com.android.application") }`
    // can fail to resolve even though `com.android.tools.build:gradle:<version>` itself
    // is available. Map the plugin ids straight onto the real module coordinates.
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application",
                "com.android.library",
                -> useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "LinuxDesktopForAndroid"

include(":app")
include(":termux-runtime")
include(":embedded-x11")

include(":terminal-emulator")
project(":terminal-emulator").projectDir = file("vendor/termux-app/terminal-emulator")

include(":terminal-view")
project(":terminal-view").projectDir = file("vendor/termux-app/terminal-view")

include(":termux-shared")
project(":termux-shared").projectDir = file("vendor/termux-app/termux-shared")

