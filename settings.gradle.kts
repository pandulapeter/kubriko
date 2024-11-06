rootProject.name = "Kubriko"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("gradle")
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
include(
    ":engine",
    ":examples:game-pong",
    ":examples:test-performance",
    ":plugins:keyboard-input-manager",
    ":plugins:serialization-manager",
    ":plugins:shader-manager",
    ":tools:debug-menu",
    ":tools:scene-editor",
)