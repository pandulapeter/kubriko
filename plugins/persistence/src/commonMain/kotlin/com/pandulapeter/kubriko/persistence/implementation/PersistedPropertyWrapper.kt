package com.pandulapeter.kubriko.persistence.implementation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal sealed class PersistedPropertyWrapper<T>(
    protected val key: kotlin.String,
    protected val defaultValue: T,
) {
    val flow = MutableStateFlow(defaultValue)

    abstract fun load(keyValuePersistenceManager: KeyValuePersistenceManager)

    abstract fun save(keyValuePersistenceManager: KeyValuePersistenceManager)

    class Boolean(
        key: kotlin.String,
        defaultValue: kotlin.Boolean,
    ) : PersistedPropertyWrapper<kotlin.Boolean>(key, defaultValue) {

        override fun load(keyValuePersistenceManager: KeyValuePersistenceManager) = flow.update { keyValuePersistenceManager.getBoolean(key, defaultValue) }

        override fun save(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.putBoolean(key, flow.value)
    }

    class Int(
        key: kotlin.String,
        defaultValue: kotlin.Int,
    ) : PersistedPropertyWrapper<kotlin.Int>(key, defaultValue) {

        override fun load(keyValuePersistenceManager: KeyValuePersistenceManager) = flow.update { keyValuePersistenceManager.getInt(key, defaultValue) }

        override fun save(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.putInt(key, flow.value)
    }

    class String(
        key: kotlin.String,
        defaultValue: kotlin.String,
    ) : PersistedPropertyWrapper<kotlin.String>(key, defaultValue) {

        override fun load(keyValuePersistenceManager: KeyValuePersistenceManager) = flow.update { keyValuePersistenceManager.getString(key, defaultValue) }

        override fun save(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.putString(key, flow.value)
    }

    class Generic<T>(
        key: kotlin.String,
        defaultValue: T,
        private val serializer: (T) -> kotlin.String,
        private val deserializer: (kotlin.String) -> T?,
    ) : PersistedPropertyWrapper<T>(key, defaultValue) {

        override fun load(keyValuePersistenceManager: KeyValuePersistenceManager) = flow.update { deserializer(keyValuePersistenceManager.getString(key, "")) ?: defaultValue }

        override fun save(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.putString(key, serializer(flow.value))
    }
}