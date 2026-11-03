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
    api(projects.creator.git)
    api(projects.creator.sceneRuntime)
    implementation(projects.creator.template)
    testFixturesApi(testFixtures(projects.creator.gradleSession))
    testImplementation(testFixtures(projects.creator.foundation))
    testImplementation(testFixtures(projects.creator.core))
    testImplementation(testFixtures(projects.creator.commands))
    testImplementation(testFixtures(projects.creator.gradleSession))
    testImplementation(testFixtures(projects.creator.git))
}
