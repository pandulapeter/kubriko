/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu.implementation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RefCountedRegistryTest {

    private var createCount = 0
    private val released = mutableListOf<Any>()
    private val registry = RefCountedRegistry<Any, Any>(
        create = {
            createCount++
            Any()
        },
        release = { released.add(it) },
    )

    @Test
    fun firstAcquireCreatesAndPublishesOnce() {
        val key = Any()
        registry.acquire(key)
        assertEquals(1, createCount)
        assertEquals(1, registry.values.value.size)
        val value = registry.values.value[key]
        registry.acquire(key)
        assertEquals(1, createCount)
        assertEquals(value, registry.values.value[key])
    }

    @Test
    fun valueIsReleasedOnlyByTheLastRelease() {
        val key = Any()
        registry.acquire(key)
        registry.acquire(key)
        val value = assertNotNull(registry.values.value[key])
        registry.release(key)
        assertEquals(value, registry.values.value[key])
        assertTrue(released.isEmpty())
        registry.release(key)
        assertNull(registry.values.value[key])
        assertEquals(listOf(value), released)
    }

    @Test
    fun distinctKeysGetDistinctValues() {
        val first = Any()
        val second = Any()
        registry.acquire(first)
        registry.acquire(second)
        assertEquals(2, createCount)
        assertEquals(2, registry.values.value.size)
        registry.release(first)
        assertNull(registry.values.value[first])
        assertNotNull(registry.values.value[second])
        assertEquals(1, released.size)
    }

    @Test
    fun releasingAnUnknownKeyIsNoOp() {
        registry.release(Any())
        assertTrue(registry.values.value.isEmpty())
        assertTrue(released.isEmpty())
    }
}
