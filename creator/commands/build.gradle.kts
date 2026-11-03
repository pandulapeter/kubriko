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
    api(projects.creator.core)
    api(projects.creator.git)
    api(projects.creator.template)
    testFixturesApi(testFixtures(projects.creator.core))
    testFixturesApi(testFixtures(projects.creator.git))
    testFixturesApi(testFixtures(projects.creator.foundation))
    testFixturesApi(libs.kotlin.test)
    testFixturesApi(libs.kotlinx.coroutines.test)
}

// The engine's overview, which get_kubriko_api hands the AI.
tasks.processResources {
    from(rootProject.file("CLAUDE.md")) { rename { "kubriko-overview.md" } }
}
