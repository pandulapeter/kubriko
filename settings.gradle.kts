rootProject.name = "Game"
include(
    ":app",
    ":engine",
    ":gameplay-controller",
    ":gameplay-objects",
    ":ui",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
