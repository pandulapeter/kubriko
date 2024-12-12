package com.pandulapeter.kubriko.persistence.implementation

import java.util.prefs.Preferences

internal actual fun createKeyValuePersistenceManager(fileName: String) = object : KeyValuePersistenceManager {
    private val preferences by lazy { Preferences.userRoot().node(fileName) }

    override fun getBoolean(key: String, defaultValue: Boolean) = preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) = preferences.putBoolean(key, value)

    override fun getInt(key: String, defaultValue: Int) = preferences.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) = preferences.putInt(key, value)

    override fun getString(key: String, defaultValue: String) = preferences.get(key, defaultValue)

    override fun putString(key: String, value: String) = preferences.put(key, value)
}