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
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(projects.plugins.serialization)
            implementation(compose.components.resources)
        }
        val desktopMain by getting {
            dependencies {
                implementation(projects.plugins.collision)
                implementation(projects.plugins.keyboardInput)
                implementation(projects.plugins.persistence)
                implementation(projects.plugins.pointerInput)
                implementation(projects.tools.debugMenu)
                implementation(projects.tools.uiComponents)
                implementation(compose.material3)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlin.reflect)
            }
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.sceneEditor"
}
