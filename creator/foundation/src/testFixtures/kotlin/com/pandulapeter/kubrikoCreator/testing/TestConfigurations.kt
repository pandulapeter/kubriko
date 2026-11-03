/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import com.pandulapeter.kubrikoCreator.foundation.AndroidVersions
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration

/**
 * The Android versions tests write into projects.
 */
val testAndroidVersions = AndroidVersions(gradlePlugin = "9.5.0", compileSdk = 37, minSdk = 29, activity = "1.13.0")

/**
 * A [CreatorConfiguration] with fixed versions, for tests that create projects without building them.
 */
fun testConfiguration(
    kubrikoVersion: String = "1.0.0",
    useMavenLocal: Boolean = false,
) = CreatorConfiguration(
    kubrikoVersion = kubrikoVersion,
    kotlinVersion = "2.4.21",
    composeVersion = "1.12.1",
    gradleVersion = "9.7.1",
    android = testAndroidVersions,
    useMavenLocal = useMavenLocal,
)
