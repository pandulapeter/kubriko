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
    id("kubriko-public-artifact")
}

artifactMetadata {
    artifactId = "plugin-serialization"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(libs.kotlinx.serialization)
        }
    }
}

android {
    namespace = "com.pandulapeter.kubriko.serialization"
}