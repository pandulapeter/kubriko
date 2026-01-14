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
    id("kubriko-public-artifact")
}

artifactMetadata {
    artifactId = "tool-scene-editor"
}

kotlin {
    androidLibrary {
        namespace = "com.pandulapeter.kubriko.sceneEditor"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.tools.sceneEditorApi)
            implementation(libs.compose.resources)
        }
        val desktopMain by getting {
            dependencies {
                implementation(projects.plugins.collision)
                implementation(projects.plugins.keyboardInput)
                implementation(projects.plugins.persistence)
                implementation(projects.plugins.pointerInput)
                implementation(if (project.findProperty("showcase.isDebugMenuEnabled") == "true") projects.tools.debugMenu else projects.tools.debugMenuNoop)
                implementation(projects.tools.uiComponents)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlin.reflect)
            }
        }
    }
}
