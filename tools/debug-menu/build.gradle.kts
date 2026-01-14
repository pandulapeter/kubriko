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
    artifactId = "tool-debug-menu"
}

kotlin {
    androidLibrary {
        namespace = "com.pandulapeter.kubriko.debugMenu"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.tools.debugMenuApi)
            implementation(projects.plugins.collision)
            implementation(projects.plugins.persistence)
            implementation(projects.tools.logger)
            implementation(projects.tools.uiComponents)
            implementation(libs.compose.resources)
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.datetime)
        }
    }
}