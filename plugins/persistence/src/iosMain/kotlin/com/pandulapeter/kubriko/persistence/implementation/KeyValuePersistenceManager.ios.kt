package com.pandulapeter.kubriko.persistence.implementation

import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue

// TODO: Default values are not handled properly
internal actual fun createKeyValuePersistenceManager(fileName: String) = object : KeyValuePersistenceManager {

    private val preferences by lazy { NSUserDefaults.standardUserDefaults }

    override fun getBoolean(key: String, defaultValue: Boolean) = preferences.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) = preferences.setBool(value, key)

    override fun getInt(key: String, defaultValue: Int) = preferences.integerForKey(key).toInt()

    override fun putInt(key: String, value: Int) = preferences.setInteger(value.toLong(), key)

    override fun getFloat(key: String, defaultValue: Float) = preferences.floatForKey(key)

    override fun putFloat(key: String, value: Float) = preferences.setFloat(value, key)

    override fun getString(key: String, defaultValue: String) = preferences.stringForKey(key) ?: defaultValue

    override fun putString(key: String, value: String) = preferences.setValue(value, key)
}