/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

/**
 * The versions a game's Android target is built with, the ones this repository builds Kubriko's Android libraries with.
 *
 * @param gradlePlugin The Android Gradle plugin's version.
 * @param compileSdk The Android API level the game compiles against and targets.
 * @param minSdk The oldest Android API level the game runs on, which can't be older than Kubriko's.
 * @param activity The version of `androidx.activity`, whose `ComponentActivity` shows the game.
 */
public data class AndroidVersions(
    val gradlePlugin: String,
    val compileSdk: Int,
    val minSdk: Int,
    val activity: String,
)
