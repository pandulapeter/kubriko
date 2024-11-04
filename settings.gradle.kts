rootProject.name = "Kubriko"
include(
    ":engine",
    ":games:pong",
    ":games:stress-test",
    ":plugins:actor-serializer",
    ":plugins:debug-info",
    ":plugins:shader",
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
