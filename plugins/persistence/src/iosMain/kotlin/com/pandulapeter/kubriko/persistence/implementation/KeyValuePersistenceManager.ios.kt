/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.persistence.implementation

import androidx.compose.runtime.Composable
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun createKeyValuePersistenceManager(fileName: String) = object : KeyValuePersistenceManager {

    private val preferences by lazy { NSUserDefaults.standardUserDefaults }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        preferences.registerDefaults(mapOf(key.prefixed() to defaultValue))
        return preferences.boolForKey(key.prefixed())
    }

    override fun putBoolean(key: String, value: Boolean) = preferences.setBool(value, key.prefixed())

    override fun getInt(key: String, defaultValue: Int): Int {
        preferences.registerDefaults(mapOf(key.prefixed() to defaultValue))
        return preferences.integerForKey(key.prefixed()).toInt()
    }

    override fun putInt(key: String, value: Int) = preferences.setInteger(value.toLong(), key.prefixed())

    override fun getFloat(key: String, defaultValue: Float): Float {
        preferences.registerDefaults(mapOf(key.prefixed() to defaultValue))
        return preferences.floatForKey(key.prefixed())
    }

    override fun putFloat(key: String, value: Float) = preferences.setFloat(value, key.prefixed())

    // Earlier versions saved strings under the bare key, so it is read once as a fallback (and never deleted, since the
    // shared defaults domain may belong to the host app).
    override fun getString(key: String, defaultValue: String) =
        preferences.stringForKey(key.prefixed()) ?: preferences.stringForKey(key) ?: defaultValue

    override fun putString(key: String, value: String) = preferences.setObject(value, forKey = key.prefixed())

    private fun String.prefixed() = "${fileName}_$this"
}