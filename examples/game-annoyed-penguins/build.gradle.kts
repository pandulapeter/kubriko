/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
plugins {
    id("kubriko-compose-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.examples.shared)
            implementation(projects.engine)
            implementation(projects.plugins.audioPlayback)
            implementation(projects.plugins.physics)
            implementation(projects.plugins.keyboardInput)
            implementation(projects.plugins.particles)
            implementation(projects.plugins.persistence)
            implementation(projects.plugins.pointerInput)
            implementation(projects.plugins.shaders)
            implementation(projects.plugins.sprites)
            implementation(projects.tools.debugMenu)
            implementation(projects.tools.sceneEditor)
            implementation(projects.tools.uiComponents)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.serialization)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.gameAnnoyedPenguins"
}