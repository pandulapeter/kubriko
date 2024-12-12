package com.pandulapeter.kubriko.persistence.implementation

import kotlinx.browser.localStorage

internal actual fun createKeyValuePersistenceManager(fileName: String) = object : KeyValuePersistenceManager {
    
    private val preferences by lazy { localStorage }

    override fun getBoolean(key: String, defaultValue: Boolean) = when (preferences.getItem(key)) {
        null, "" -> defaultValue
        "true", "TRUE", "True", "1" -> true
        else -> false
    }

    override fun putBoolean(key: String, value: Boolean) = preferences.setItem(key, if (value) "true" else "false")

    override fun getInt(key: String, defaultValue: Int) = try {
        preferences.getItem(key)?.toInt() ?: defaultValue
    } catch (_: NumberFormatException) {
        defaultValue
    }

    override fun putInt(key: String, value: Int) = preferences.setItem(key, value.toString())

    override fun getString(key: String, defaultValue: String) = preferences.getItem(key) ?: defaultValue

    override fun putString(key: String, value: String) = preferences.setItem(key, value)
}