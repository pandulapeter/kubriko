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
    ":plugins:actor-serializer",
    ":plugins:debug-info",
    ":plugins:input-manager",
    ":plugins:shader-manager",
    ":tools:scene-editor",
)