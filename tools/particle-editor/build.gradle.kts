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
        val desktopMain by getting {
            dependencies {
                implementation(projects.engine)
                implementation(compose.components.resources)
                implementation(projects.plugins.particles)
                implementation(projects.plugins.pointerInput)
                implementation(projects.tools.uiComponents)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.particleEditor"
}
