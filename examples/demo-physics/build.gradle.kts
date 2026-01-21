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
    id("kubriko-compose-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.pandulapeter.kubriko.demoPhysics"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.examples.shared)
            implementation(projects.engine)
            implementation(projects.plugins.physics)
            implementation(projects.plugins.pointerInput)
            implementation(if (project.findProperty("showcase.isDebugMenuEnabled") == "true") projects.tools.debugMenu else projects.tools.debugMenuNoop)
            implementation(if (project.findProperty("showcase.isSceneEditorEnabled") == "true") projects.tools.sceneEditor else projects.tools.sceneEditorNoop)
            implementation(projects.tools.uiComponents)
            implementation(libs.compose.resources)
            implementation(libs.kotlinx.serialization)
        }
    }
}