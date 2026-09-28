/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.persistence

import com.pandulapeter.kubriko.persistence.implementation.KeyValuePersistenceManager
import com.pandulapeter.kubriko.persistence.implementation.PersistedPropertyWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistedPropertyWrapperTest {

    @Test
    fun genericWithEmptyStoreUsesDefaultWithoutCallingDeserializer() {
        val deserializerCalls = mutableListOf<String>()
        val wrapper = PersistedPropertyWrapper.Generic(
            key = KEY,
            defaultValue = DEFAULT,
            serializer = { it },
            deserializer = { deserializerCalls.add(it); it },
        )

        assertTrue(wrapper.load(InMemoryKeyValuePersistenceManager()))
        assertEquals(DEFAULT, wrapper.flow.value)
        assertTrue(deserializerCalls.isEmpty())
    }

    @Test
    fun genericWithUnreadableStoredValueFallsBackToDefault() {
        val store = InMemoryKeyValuePersistenceManager().apply { putString(KEY, "garbage") }
        val wrapper = PersistedPropertyWrapper.Generic<String>(
            key = KEY,
            defaultValue = DEFAULT,
            serializer = { it },
            deserializer = { throw IllegalArgumentException("Cannot parse $it") },
        )

        assertFalse(wrapper.load(store))
        assertEquals(DEFAULT, wrapper.flow.value)
    }

    @Test
    fun genericWithValidStoredValueDeserializesIt() {
        val store = InMemoryKeyValuePersistenceManager().apply { putString(KEY, "42") }
        val wrapper = PersistedPropertyWrapper.Generic(
            key = KEY,
            defaultValue = 0,
            serializer = { it.toString() },
            deserializer = { it.toInt() },
        )

        assertTrue(wrapper.load(store))
        assertEquals(42, wrapper.flow.value)
    }

    @Test
    fun failedWriteDoesNotThrowAndLaterSaveSucceeds() {
        val wrapper = PersistedPropertyWrapper.String(KEY, DEFAULT)
        wrapper.flow.value = "changed"
        var reportedFailure: Throwable? = null

        assertFalse(wrapper.save(InMemoryKeyValuePersistenceManager(shouldThrowOnPut = true)) { reportedFailure = it })
        assertTrue(reportedFailure != null)

        val store = InMemoryKeyValuePersistenceManager()
        assertTrue(wrapper.save(store))
        assertEquals("changed", store.getStringOrNull(KEY))
    }

    @Test
    fun throwingSerializerDoesNotThrow() {
        val store = InMemoryKeyValuePersistenceManager()
        val wrapper = PersistedPropertyWrapper.Generic<String>(
            key = KEY,
            defaultValue = DEFAULT,
            serializer = { throw IllegalStateException("Cannot serialize $it") },
            deserializer = { it },
        )

        assertFalse(wrapper.save(store))
        assertEquals(null, store.getStringOrNull(KEY))
    }

    private class InMemoryKeyValuePersistenceManager(
        private val shouldThrowOnPut: Boolean = false,
    ) : KeyValuePersistenceManager {
        private val values = mutableMapOf<String, Any>()

        override fun getBoolean(key: String, defaultValue: Boolean) = values[key] as? Boolean ?: defaultValue

        override fun putBoolean(key: String, value: Boolean) = put(key, value)

        override fun getInt(key: String, defaultValue: Int) = values[key] as? Int ?: defaultValue

        override fun putInt(key: String, value: Int) = put(key, value)

        override fun getFloat(key: String, defaultValue: Float) = values[key] as? Float ?: defaultValue

        override fun putFloat(key: String, value: Float) = put(key, value)

        override fun getString(key: String, defaultValue: String) = getStringOrNull(key) ?: defaultValue

        override fun getStringOrNull(key: String) = values[key] as? String

        override fun putString(key: String, value: String) = put(key, value)

        private fun put(key: String, value: Any) {
            if (shouldThrowOnPut) throw IllegalStateException("Storage refused \"$key\"")
            values[key] = value
        }
    }

    private companion object {
        const val KEY = "key"
        const val DEFAULT = "default"
    }
}
