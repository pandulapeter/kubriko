/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.persistence.implementation

import kotlinx.coroutines.flow.MutableStateFlow

internal sealed class PersistedPropertyWrapper<T>(
    val key: kotlin.String,
    protected val defaultValue: T,
) {
    val flow = MutableStateFlow(defaultValue)

    /**
     * Replaces the value with the stored one. Never throws: on failure the value is left unchanged (initially [defaultValue]), [onFailure] is
     * called and `false` is returned.
     */
    fun load(
        keyValuePersistenceManager: KeyValuePersistenceManager,
        onFailure: (Throwable) -> Unit = {},
    ) = try {
        flow.value = readValue(keyValuePersistenceManager)
        true
    } catch (throwable: Throwable) {
        onFailure(throwable)
        false
    }

    /**
     * Stores the current value. Never throws: on failure the storage is left as it was, [onFailure] is called and
     * `false` is returned.
     */
    fun save(
        keyValuePersistenceManager: KeyValuePersistenceManager,
        onFailure: (Throwable) -> Unit = {},
    ) = try {
        writeValue(keyValuePersistenceManager)
        true
    } catch (throwable: Throwable) {
        onFailure(throwable)
        false
    }

    protected abstract fun readValue(keyValuePersistenceManager: KeyValuePersistenceManager): T

    protected abstract fun writeValue(keyValuePersistenceManager: KeyValuePersistenceManager)

    class Boolean(
        key: kotlin.String,
        defaultValue: kotlin.Boolean,
    ) : PersistedPropertyWrapper<kotlin.Boolean>(key, defaultValue) {

        override fun readValue(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.getBoolean(key, defaultValue)

        override fun writeValue(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.putBoolean(key, flow.value)
    }

    class Int(
        key: kotlin.String,
        defaultValue: kotlin.Int,
    ) : PersistedPropertyWrapper<kotlin.Int>(key, defaultValue) {

        override fun readValue(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.getInt(key, defaultValue)

        override fun writeValue(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.putInt(key, flow.value)
    }

    class Float(
        key: kotlin.String,
        defaultValue: kotlin.Float,
    ) : PersistedPropertyWrapper<kotlin.Float>(key, defaultValue) {

        override fun readValue(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.getFloat(key, defaultValue)

        override fun writeValue(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.putFloat(key, flow.value)
    }

    class String(
        key: kotlin.String,
        defaultValue: kotlin.String,
    ) : PersistedPropertyWrapper<kotlin.String>(key, defaultValue) {

        override fun readValue(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.getString(key, defaultValue)

        override fun writeValue(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.putString(key, flow.value)
    }

    class Generic<T>(
        key: kotlin.String,
        defaultValue: T,
        private val serializer: (T) -> kotlin.String,
        private val deserializer: (kotlin.String) -> T?,
    ) : PersistedPropertyWrapper<T>(key, defaultValue) {

        override fun readValue(keyValuePersistenceManager: KeyValuePersistenceManager) =
            keyValuePersistenceManager.getStringOrNull(key)?.let(deserializer) ?: defaultValue

        override fun writeValue(keyValuePersistenceManager: KeyValuePersistenceManager) = keyValuePersistenceManager.putString(key, serializer(flow.value))
    }
}