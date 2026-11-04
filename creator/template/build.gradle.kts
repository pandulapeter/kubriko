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
    implementation(projects.creator.git)
    implementation(libs.gradle.toolingApi)
    implementation(libs.jgit)
    testImplementation(projects.creator.gradleSession)
    testImplementation(testFixtures(projects.creator.core))
    testImplementation(testFixtures(projects.creator.foundation))
}

// Projects get the same Public Sans file as the Showcase's components, copied in rather than kept twice.
tasks.processResources {
    from("../../tools/ui-components/src/commonMain/composeResources/font/public_sans_regular.ttf") { into("fonts") }
}
