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
    ":app",
    ":engine",
    ":examples:demo-custom-shaders",
    ":examples:demo-input",
    ":examples:demo-performance",
    ":examples:demo-physics",
    ":examples:game-wallbreaker",
    ":plugins:keyboard-input",
    ":plugins:physics",
    ":plugins:serialization",
    ":plugins:shader",
    ":tools:debug-menu",
    ":tools:scene-editor",
)