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

import androidx.compose.runtime.Composable
import kotlinx.browser.localStorage

@Composable
internal actual fun createKeyValuePersistenceManager(fileName: String) = object : KeyValuePersistenceManager {

    private val preferences by lazy { localStorage }

    override fun getBoolean(key: String, defaultValue: Boolean) = when (preferences.getItem(key.prefixed())) {
        null, "" -> defaultValue
        "true", "TRUE", "True", "1" -> true
        else -> false
    }

    override fun putBoolean(key: String, value: Boolean) = preferences.setItem(key.prefixed(), if (value) "true" else "false")

    override fun getInt(key: String, defaultValue: Int) = preferences.getItem(key.prefixed())?.toIntOrNull() ?: defaultValue

    override fun putInt(key: String, value: Int) = preferences.setItem(key.prefixed(), value.toString())

    override fun getFloat(key: String, defaultValue: Float) = preferences.getItem(key.prefixed())?.toFloatOrNull() ?: defaultValue

    override fun putFloat(key: String, value: Float) = preferences.setItem(key.prefixed(), value.toString())

    override fun getString(key: String, defaultValue: String) = preferences.getItem(key.prefixed()) ?: defaultValue

    override fun putString(key: String, value: String) = preferences.setItem(key.prefixed(), value)

    private fun String.prefixed() = "${fileName}_$this"
}