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
        maven { url = uri("https://jitpack.io") }
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
    ":examples:game-space-squadron",
    ":examples:game-wallbreaker",
    ":examples:shared",
    ":plugins:audio-player",
    ":plugins:collision",
    ":plugins:keyboard-input",
    ":plugins:persistence",
    ":plugins:physics",
    ":plugins:pointer-input",
    ":plugins:serialization",
    ":plugins:shader",
    ":tools:debug-menu",
    ":tools:scene-editor",
)