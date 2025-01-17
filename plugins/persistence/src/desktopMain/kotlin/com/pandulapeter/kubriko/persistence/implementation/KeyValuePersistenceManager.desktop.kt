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
import java.util.prefs.Preferences

@Composable
internal actual fun createKeyValuePersistenceManager(fileName: String) = object : KeyValuePersistenceManager {

    private val preferences by lazy { Preferences.userRoot().node(fileName) }

    override fun getBoolean(key: String, defaultValue: Boolean) = preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) = preferences.putBoolean(key, value)

    override fun getInt(key: String, defaultValue: Int) = preferences.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) = preferences.putInt(key, value)

    override fun getFloat(key: String, defaultValue: Float) = preferences.getFloat(key, defaultValue)

    override fun putFloat(key: String, value: Float) = preferences.putFloat(key, value)

    override fun getString(key: String, defaultValue: String) = preferences.get(key, defaultValue)

    override fun putString(key: String, value: String) = preferences.put(key, value)
}