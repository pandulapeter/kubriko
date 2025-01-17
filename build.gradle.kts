/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
"SHOWCASE_ANDROID_VERSION_NAME" set "1.0.0"
"SHOWCASE_ANDROID_VERSION_CODE" set 1
"SHOWCASE_ANDROID_KEY_ALIAS" set "androiddebugkey"
"SHOWCASE_ANDROID_KEY_PASSWORD" set "android"
"SHOWCASE_ANDROID_STORE_FILE" set "internal.keystore"
"SHOWCASE_ANDROID_STORE_PASSWORD" set "android"
"SHOWCASE_DEKTOP_VERSION_NAME" set "1.0.0"

infix fun String.set(value: Any) = System.setProperty(this, value.toString())

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}