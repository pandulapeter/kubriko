/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
import com.android.build.api.dsl.androidLibrary

plugins {
    id("kubriko-compose-library")
}

kotlin {
    @Suppress("UnstableApiUsage")
    androidLibrary {
        namespace = "com.pandulapeter.kubriko.gameSpaceSquadron"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.examples.shared)
            implementation(projects.engine)
            implementation(projects.plugins.audioPlayback)
            implementation(projects.plugins.collision)
            implementation(projects.plugins.keyboardInput)
            implementation(projects.plugins.particles)
            implementation(projects.plugins.persistence)
            implementation(projects.plugins.pointerInput)
            implementation(projects.plugins.shaders)
            implementation(projects.plugins.sprites)
            implementation(if (project.findProperty("showcase.isDebugMenuEnabled") == "true") projects.tools.debugMenu else projects.tools.debugMenuNoop)
            implementation(projects.tools.uiComponents)
            implementation(libs.compose.resources)
        }
    }
}