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
    ":examples:test-keyboard-input",
    ":examples:test-performance",
    ":examples:test-physics",
    ":plugins:keyboard-input",
    ":plugins:physics",
    ":plugins:serialization",
    ":plugins:shader",
    ":tools:debug-menu",
    ":tools:scene-editor",
)