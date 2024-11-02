rootProject.name = "Kubriko"
include(
    ":engine",
    ":games:pong",
    ":games:stress-test",
    ":plugins:debug-info",
    ":tools:scene-editor",
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
