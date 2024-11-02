rootProject.name = "Kubriko"
include(
    ":engine",
    ":games:pong",
    ":games:stress-test",
    ":scene-editor",
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
