package com.pandulapeter.kubriko.persistence

import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * TODO: Documentation
 */
sealed class PersistenceManager : Manager() {

    abstract fun boolean(
        key: String,
        defaultValue: Boolean = false,
    ): MutableStateFlow<Boolean>

    abstract fun int(
        key: String,
        defaultValue: Int = 0,
    ): MutableStateFlow<Int>

    abstract fun float(
        key: String,
        defaultValue: Float = 0f,
    ): MutableStateFlow<Float>

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