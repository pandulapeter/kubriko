package com.pandulapeter.kubriko.persistence.implementation

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

internal expect fun createKeyValuePersistenceManager(fileName: String): KeyValuePersistenceManager