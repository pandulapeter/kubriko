package com.pandulapeter.kubriko.persistence.implementation

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal sealed class PersistedPropertyDelegate<T> : ReadWriteProperty<Any, T> {

    protected abstract val keyValuePersistenceManager: KeyValuePersistenceManager
    protected abstract val key: kotlin.String

    class Boolean(
        override val keyValuePersistenceManager: KeyValuePersistenceManager,
        override val key: kotlin.String,
        private val defaultValue: kotlin.Boolean,
    ) : PersistedPropertyDelegate<kotlin.Boolean>() {
        override fun getValue(thisRef: Any, property: KProperty<*>) = keyValuePersistenceManager.getBoolean(key, defaultValue)

        override fun setValue(thisRef: Any, property: KProperty<*>, value: kotlin.Boolean) = keyValuePersistenceManager.putBoolean(key, value)
    }

    class Int(
        override val keyValuePersistenceManager: KeyValuePersistenceManager,
        override val key: kotlin.String,
        private val defaultValue: kotlin.Int,
    ) : PersistedPropertyDelegate<kotlin.Int>() {
        override fun getValue(thisRef: Any, property: KProperty<*>) = keyValuePersistenceManager.getInt(key, defaultValue)

        override fun setValue(thisRef: Any, property: KProperty<*>, value: kotlin.Int) = keyValuePersistenceManager.putInt(key, value)
    }

    class String(
        override val keyValuePersistenceManager: KeyValuePersistenceManager,
        override val key: kotlin.String,
        private val defaultValue: kotlin.String,
    ) : PersistedPropertyDelegate<kotlin.String>() {
        override fun getValue(thisRef: Any, property: KProperty<*>) = keyValuePersistenceManager.getString(key, defaultValue)

        override fun setValue(thisRef: Any, property: KProperty<*>, value: kotlin.String) = keyValuePersistenceManager.putString(key, value)
    }

    class Generic<T>(
        override val keyValuePersistenceManager: KeyValuePersistenceManager,
        override val key: kotlin.String,
        private val serializer: (T) -> kotlin.String,
        private val deserializer: (kotlin.String) -> T,
    ) : PersistedPropertyDelegate<T>() {

        override fun getValue(thisRef: Any, property: KProperty<*>) = deserializer(keyValuePersistenceManager.getString(key, ""))

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) = keyValuePersistenceManager.putString(key, serializer(value))
    }
}