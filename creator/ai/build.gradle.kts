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
}

dependencies {
    api(projects.creator.commands)
    // The assistant's transcript, plans, and permission requests carry the ACP SDK's model types to the app's panel.
    api(libs.acp)
    implementation(projects.creator.gradleSession)
    implementation(libs.ktor.server.cio)
    implementation(libs.mcp.server)
    testFixturesImplementation(testFixtures(projects.creator.foundation))
    testImplementation(testFixtures(projects.creator.commands))
    testImplementation(testFixtures(projects.creator.gradleSession))
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.mcp.client)
}
