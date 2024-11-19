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
    ":examples:pong",
    ":examples:showcase",
    ":examples:test-keyboard-input",
    ":examples:test-performance",
    ":examples:test-physics",
    ":examples:test-shader",
    ":plugins:keyboard-input",
    ":plugins:physics",
    ":plugins:serialization",
    ":plugins:shader",
    ":tools:debug-menu",
    ":tools:scene-editor",
)