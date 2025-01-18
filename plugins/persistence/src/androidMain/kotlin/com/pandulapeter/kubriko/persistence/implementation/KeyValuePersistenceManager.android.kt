/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.persistence.implementation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun createKeyValuePersistenceManager(fileName: String) = object : KeyValuePersistenceManager {

    private val preferences = LocalContext.current.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    override fun getBoolean(key: String, defaultValue: Boolean) = preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()

    override fun getInt(key: String, defaultValue: Int) = preferences.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) = preferences.edit().putInt(key, value).apply()

    override fun getFloat(key: String, defaultValue: Float) = preferences.getFloat(key, defaultValue)

    override fun putFloat(key: String, value: Float) = preferences.edit().putFloat(key, value).apply()

    override fun getString(key: String, defaultValue: String) = preferences.getString(key, defaultValue) ?: defaultValue

    override fun putString(key: String, value: String) = preferences.edit().putString(key, value).apply()
}