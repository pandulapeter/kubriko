/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.persistence.implementation

import androidx.compose.runtime.Composable

internal interface KeyValuePersistenceManager {

    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)

    fun getInt(key: String, defaultValue: Int): Int

    fun putInt(key: String, value: Int)

    fun getFloat(key: String, defaultValue: Float): Float

    fun putFloat(key: String, value: Float)

    fun getString(key: String, defaultValue: String): String

    fun putString(key: String, value: String)
}

@Composable
internal expect fun createKeyValuePersistenceManager(fileName: String): KeyValuePersistenceManager