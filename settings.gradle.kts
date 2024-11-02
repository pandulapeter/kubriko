rootProject.name = "Kubriko"
include(
    ":editor",
    ":engine",
    ":game-pong",
    ":game-stress-test",
    ":plugins:debug-info",
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
