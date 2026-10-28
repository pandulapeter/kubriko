/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
// Kubriko Creator's development run publishes the libraries to Maven Local under a version of their own, so that they never
// stand in for a real release in other projects (see creator/CLAUDE.md).
version = project.findProperty("library.version").toString() + if (project.findProperty("creator.localKubriko") == "true") "-creator" else ""

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.multiplatformLibrary) apply false
    alias(libs.plugins.codingfeline.buildkonfig) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.desktop) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    id("kubriko-style")
}
