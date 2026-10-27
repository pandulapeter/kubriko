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
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.desktop)
}

dependencies {
    implementation(projects.tools.creatorSession)
    implementation(projects.tools.uiComponents)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    implementation(libs.gradle.toolingApi)
    implementation(libs.kotlinx.coroutines.desktop)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

compose.desktop {
    application {
        mainClass = "com.pandulapeter.kubrikoCreator.KubrikoCreatorAppKt"
        nativeDistributions {
            packageName = "Kubriko Creator"
            packageVersion = "1.0.0"
        }
    }
}
