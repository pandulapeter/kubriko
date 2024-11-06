rootProject.name = "Kubriko"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
include(
    ":engine",
    ":games:pong",
    ":games:stress-test",
    ":plugins:keyboard-input-manager",
    ":plugins:serialization-manager",
    ":plugins:shader-manager",
    ":tools:debug-menu",
    ":tools:scene-editor",
)