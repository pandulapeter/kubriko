package com.pandulapeter.kubriko.persistence.implementation

import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue

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

    override fun getString(key: String, defaultValue: String): String {
        preferences.registerDefaults(mapOf(key.prefixed() to defaultValue))
        return preferences.stringForKey(key.prefixed()) ?: defaultValue
    }

    override fun putString(key: String, value: String) = preferences.setValue(value, key)

    private fun String.prefixed() = "${fileName}_$this"
}