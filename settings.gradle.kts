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
    ":examples:demo-audio",
    ":examples:demo-content-shaders",
    ":examples:demo-input",
    ":examples:demo-performance",
    ":examples:demo-physics",
    ":examples:demo-shader-animations",
    ":examples:game-space-squadron",
    ":examples:game-wallbreaker",
    ":examples:shared",
    ":plugins:audio-playback",
    ":plugins:collision",
    ":plugins:keyboard-input",
    ":plugins:particles",
    ":plugins:persistence",
    ":plugins:physics",
    ":plugins:pointer-input",
    ":plugins:serialization",
    ":plugins:shaders",
    ":plugins:sprites",
    ":tools:debug-menu",
    ":tools:scene-editor",
)