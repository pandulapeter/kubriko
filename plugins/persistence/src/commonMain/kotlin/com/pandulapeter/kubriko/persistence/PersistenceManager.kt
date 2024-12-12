package com.pandulapeter.kubriko.persistence

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.persistence.implementation.PersistenceManagerImpl
import kotlin.properties.ReadWriteProperty

/**
 * TODO: Documentation
 */
abstract class PersistenceManager : Manager() {

    abstract fun boolean(key: String, defaultValue: Boolean = false): ReadWriteProperty<Any, Boolean>

    abstract fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    abstract fun putBoolean(key: String, value: Boolean)

    abstract fun int(key: String, defaultValue: Int = 0): ReadWriteProperty<Any, Int>

    abstract fun getInt(key: String, defaultValue: Int = 0): Int

    abstract fun putInt(key: String, value: Int)

    abstract fun string(key: String, defaultValue: String = ""): ReadWriteProperty<Any, String>

    abstract fun getString(key: String, defaultValue: String = ""): String

    abstract fun putString(key: String, value: String)

    abstract fun <T> generic(key: String, serializer: (T) -> String, deserializer: (String) -> T): ReadWriteProperty<Any, T>

    companion object {
        fun newInstance(
            fileName: String = "kubrikoPreferences"
        ): PersistenceManager = PersistenceManagerImpl(
            fileName = fileName,
        )
    }
}