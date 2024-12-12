package com.pandulapeter.kubriko.persistence.implementation

import com.pandulapeter.kubriko.persistence.PersistenceManager

internal class PersistenceManagerImpl(
    fileName: String,
) : PersistenceManager() {

    private val keyValuePersistenceManager by lazy { createKeyValuePersistenceManager(fileName = fileName) }

    override fun boolean(key: String, defaultValue: Boolean) = PersistedPropertyDelegate.Boolean(
        keyValuePersistenceManager = keyValuePersistenceManager,
        key = key,
        defaultValue = defaultValue,
    )

    override fun getBoolean(key: String, defaultValue: Boolean) = keyValuePersistenceManager.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) = keyValuePersistenceManager.putBoolean(key, value)

    override fun int(key: String, defaultValue: Int) = PersistedPropertyDelegate.Int(
        keyValuePersistenceManager = keyValuePersistenceManager,
        key = key,
        defaultValue = defaultValue,
    )

    override fun getInt(key: String, defaultValue: Int) = keyValuePersistenceManager.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) = keyValuePersistenceManager.putInt(key, value)

    override fun string(key: String, defaultValue: String) = PersistedPropertyDelegate.String(
        keyValuePersistenceManager = keyValuePersistenceManager,
        key = key,
        defaultValue = defaultValue,
    )

    override fun getString(key: String, defaultValue: String) = keyValuePersistenceManager.getString(key, defaultValue)

    override fun putString(key: String, value: String) = keyValuePersistenceManager.putString(key, value)

    override fun <T> generic(key: String, serializer: (T) -> String, deserializer: (String) -> T) = PersistedPropertyDelegate.Generic(
        keyValuePersistenceManager = keyValuePersistenceManager,
        key = key,
        serializer = serializer,
        deserializer = deserializer,
    )
}