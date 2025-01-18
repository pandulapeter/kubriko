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
            implementation(projects.plugins.persistence)
            implementation(projects.tools.uiComponents)
            implementation(compose.components.resources)
            implementation(compose.material3)
            implementation(libs.kotlinx.datetime)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.debugMenu"
}