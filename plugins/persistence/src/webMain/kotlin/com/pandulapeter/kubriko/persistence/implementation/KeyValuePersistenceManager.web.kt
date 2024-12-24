package com.pandulapeter.kubriko.persistence.implementation

import kotlinx.browser.localStorage

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