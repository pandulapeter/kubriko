package com.pandulapeter.kubriko.persistence

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.persistence.implementation.PersistenceManagerImpl
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * TODO: Documentation
 */
abstract class PersistenceManager : Manager() {

    abstract fun boolean(
        key: String,
        defaultValue: Boolean = false,
    ): MutableStateFlow<Boolean>

    abstract fun int(
        key: String,
        defaultValue: Int = 0,
    ): MutableStateFlow<Int>

    abstract fun string(
        key: String,
        defaultValue: String = "",
    ): MutableStateFlow<String>

    abstract fun <T> generic(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): MutableStateFlow<T>

    companion object {
        fun newInstance(
            fileName: String = "kubrikoPreferences"
        ): PersistenceManager = PersistenceManagerImpl(
            fileName = fileName,
        )
    }
}