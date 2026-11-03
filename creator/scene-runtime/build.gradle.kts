/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("kubriko-creator-module")
}

dependencies {
    api(projects.creator.core)
    api(projects.creator.gradleSession)
    api(projects.engine)
    api(libs.compose.ui)
    // Every published module a project could use, so that a project's classes loaded next to Creator's share them
    // (sceneRuntime/ClassLoaderEntryPointLoader.kt).
    implementation(projects.plugins.audioPlayback)
    implementation(projects.plugins.collision)
    implementation(projects.plugins.gamepadInput)
    implementation(projects.plugins.keyboardInput)
    implementation(projects.plugins.particles)
    implementation(projects.plugins.persistence)
    implementation(projects.plugins.physics)
    implementation(projects.plugins.pointerInput)
    implementation(projects.plugins.serialization)
    implementation(projects.plugins.shaders)
    implementation(projects.plugins.sprites)
    api(projects.tools.creatorApi)
    testImplementation(testFixtures(projects.creator.foundation))
    // Skia's native library, which decoding a sprite needs; the app brings its own through compose.desktop.currentOs.
    testRuntimeOnly(org.jetbrains.compose.ComposePlugin.DesktopDependencies.currentOs)
}
