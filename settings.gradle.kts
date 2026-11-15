rootProject.name = "vantage"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":openevnt")
include(":core")
include(":storage")
include(":ui")
include(":app")
include(":androidApp")
