/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
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
    ":examples:demo-particles",
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
    ":tools:logger",
    ":tools:scene-editor",
    ":tools:ui-components",
)