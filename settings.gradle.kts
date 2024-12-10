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
    ":examples:demo-built-in-shaders",
    ":examples:demo-custom-shaders",
    ":examples:demo-input",
    ":examples:demo-performance",
    ":examples:demo-physics",
    ":examples:game-wallbreaker",
    ":plugins:audio-player",
    ":plugins:collision",
    ":plugins:keyboard-input",
    ":plugins:physics",
    ":plugins:pointer-input",
    ":plugins:serialization",
    ":plugins:shader",
    ":tools:debug-menu",
    ":tools:scene-editor",
)