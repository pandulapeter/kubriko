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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersistedPropertyWrapperTest {

    @Test
    fun savedValuesAreLoadedByTheNextWrapperOfTheKey() {
        val store = InMemoryKeyValuePersistenceManager()
        PersistedPropertyWrapper.Boolean("boolean", true).apply { flow.value = false }.save(store)
        PersistedPropertyWrapper.Int("int", 0).apply { flow.value = 42 }.save(store)
        PersistedPropertyWrapper.Float("float", 1f).apply { flow.value = 0.5f }.save(store)
        PersistedPropertyWrapper.String("string", DEFAULT).apply { flow.value = "changed" }.save(store)
        PersistedPropertyWrapper.Generic("generic", 0, serializer = { it.toString() }, deserializer = { it.toInt() }).apply { flow.value = 7 }.save(store)

        assertEquals(false, PersistedPropertyWrapper.Boolean("boolean", true).loaded(store))
        assertEquals(42, PersistedPropertyWrapper.Int("int", 0).loaded(store))
        assertEquals(0.5f, PersistedPropertyWrapper.Float("float", 1f).loaded(store))
        assertEquals("changed", PersistedPropertyWrapper.String("string", DEFAULT).loaded(store))
        assertEquals(7, PersistedPropertyWrapper.Generic("generic", 0, serializer = { it.toString() }, deserializer = { it.toInt() }).loaded(store))
    }

    @Test
    fun emptyStoreLoadsTheDefaults() {
        val store = InMemoryKeyValuePersistenceManager()

        assertEquals(true, PersistedPropertyWrapper.Boolean("boolean", true).loaded(store))
        assertEquals(3, PersistedPropertyWrapper.Int("int", 3).loaded(store))
        assertEquals(0.5f, PersistedPropertyWrapper.Float("float", 0.5f).loaded(store))
        assertEquals(DEFAULT, PersistedPropertyWrapper.String("string", DEFAULT).loaded(store))
    }

    @Test
    fun genericDeserializerReturningNullFallsBackToDefault() {
        val store = InMemoryKeyValuePersistenceManager().apply { putString(KEY, "unknown") }
        val wrapper = PersistedPropertyWrapper.Generic<String>(
            key = KEY,
            defaultValue = DEFAULT,
            serializer = { it },
            deserializer = { null },
        )

        assertTrue(wrapper.load(store))
        assertEquals(DEFAULT, wrapper.flow.value)
    }

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
        assertNotNull(reportedFailure)

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
        assertNull(store.getStringOrNull(KEY))
    }

    private fun <T> PersistedPropertyWrapper<T>.loaded(store: KeyValuePersistenceManager): T {
        assertTrue(load(store))
        return flow.value
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
