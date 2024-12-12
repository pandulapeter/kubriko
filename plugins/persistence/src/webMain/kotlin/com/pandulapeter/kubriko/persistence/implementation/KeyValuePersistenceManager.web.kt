package com.pandulapeter.kubriko.persistence.implementation

import kotlinx.browser.localStorage

internal actual fun createKeyValuePersistenceManager(fileName: String) = object : KeyValuePersistenceManager {

    override fun getBoolean(key: String, defaultValue: Boolean) = when (localStorage.getItem(key)) {
        null, "" -> defaultValue
        "true", "TRUE", "True", "1" -> true
        else -> false
    }

    override fun putBoolean(key: String, value: Boolean) = localStorage.setItem(key, if (value) "true" else "false")

    override fun getInt(key: String, defaultValue: Int) = try {
        localStorage.getItem(key)?.toInt() ?: defaultValue
    } catch (_: NumberFormatException) {
        defaultValue
    }

    override fun putInt(key: String, value: Int) = localStorage.setItem(key, value.toString())

    override fun getString(key: String, defaultValue: String) = localStorage.getItem(key) ?: defaultValue

    override fun putString(key: String, value: String) = localStorage.setItem(key, value)
}