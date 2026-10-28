/*
* This file is part of Kubriko.
* Copyright (c) Pandula Péter 2025-2026.
* https://github.com/pandulapeter/kubriko
*
* This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
* If a copy of the MPL was not distributed with this file, You can obtain one at
* https://mozilla.org/MPL/2.0/.
*/
rootProject.name = "Kubriko"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Kubriko Creator run from this repository always uses this repository's libraries, published to Maven Local first, so
// that the projects it opens never point at a Kubriko version that isn't released yet (creator/CLAUDE.md). An explicit
// -Pcreator.localKubriko=false runs it against the released libraries instead.
gradle.startParameter.let { startParameter ->
    val creatorRunTasks = setOf(":creator:run", "creator:run")
    if (startParameter.taskNames.none { it in creatorRunTasks } || startParameter.projectProperties["creator.localKubriko"] == "false") return@let
    val publishTasks = listOf(
        "publishDesktopPublicationToMavenLocal",
        "publishWasmJsPublicationToMavenLocal",
        "publishKotlinMultiplatformPublicationToMavenLocal",
    )
    startParameter.setTaskNames(publishTasks + startParameter.taskNames.filter { it !in publishTasks })
    startParameter.projectProperties = startParameter.projectProperties + ("creator.localKubriko" to "true")
}
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("gradle")
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://repo.gradle.org/gradle/libs-releases")
            content { includeGroup("org.gradle") }
        }
    }
}
include(
    ":app:android",
    ":app:desktop",
    ":app:ios",
    ":app:shared",
    ":app:web",
    ":creator",
    ":engine",
    ":examples:demo-content-shaders",
    ":examples:demo-isometric-graphics",
    ":examples:demo-particles",
    ":examples:demo-performance",
    ":examples:demo-physics",
    ":examples:demo-shader-animations",
    ":examples:game-annoyed-penguins",
    ":examples:game-blockys-journey",
    ":examples:game-space-squadron",
    ":examples:game-wallbreaker",
    ":examples:test-audio",
    ":examples:test-audio-noop",
    ":examples:test-collision",
    ":examples:test-collision-noop",
    ":examples:test-input",
    ":examples:test-input-noop",
    ":examples:shared",
    ":plugins:audio-playback",
    ":plugins:collision",
    ":plugins:gamepad-input",
    ":plugins:keyboard-input",
    ":plugins:particles",
    ":plugins:persistence",
    ":plugins:physics",
    ":plugins:pointer-input",
    ":plugins:serialization",
    ":plugins:shaders",
    ":plugins:sprites",
    ":tools:creator-api",
    ":tools:creator-session",
    ":tools:debug-menu",
    ":tools:debug-menu-api",
    ":tools:debug-menu-noop",
    ":tools:logger",
    ":tools:scene-editor",
    ":tools:scene-editor-api",
    ":tools:scene-editor-noop",
    ":tools:test-fixtures",
    ":tools:ui-components",
)
