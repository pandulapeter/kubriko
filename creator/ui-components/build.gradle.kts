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
    id("kubriko-creator-module")
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    api(projects.creator.foundation)
    api(projects.tools.uiComponents)
    api(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    testFixturesApi(compose.desktop.uiTestJUnit4)
    testFixturesApi(libs.kotlin.test)
}

compose.resources {
    packageOfResClass = "com.pandulapeter.kubrikoCreator.uiComponents.generated.resources"
    publicResClass = false
}
