/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders.extensions

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UniformCacheTest {

    @Test
    fun firstValueOfAUniformIsAChange() {
        val cache = UniformCache()

        assertTrue(cache.hasChanged("a", UniformCache.KIND_FLOAT, 1f.toRawBits()))
        assertTrue(cache.hasChanges)
    }

    @Test
    fun repeatedValueIsNotAChange() {
        val cache = UniformCache()
        cache.hasChanged("a", UniformCache.KIND_FLOAT2, 1f.toRawBits(), 2f.toRawBits())
        cache.hasChanges = false

        assertFalse(cache.hasChanged("a", UniformCache.KIND_FLOAT2, 1f.toRawBits(), 2f.toRawBits()))
        assertFalse(cache.hasChanges)
    }

    @Test
    fun differentValueOrKindIsAChange() {
        val cache = UniformCache()
        cache.hasChanged("a", UniformCache.KIND_FLOAT2, 1f.toRawBits(), 2f.toRawBits())

        assertTrue(cache.hasChanged("a", UniformCache.KIND_FLOAT2, 1f.toRawBits(), 3f.toRawBits()))
        assertTrue(cache.hasChanged("a", UniformCache.KIND_INT, 1f.toRawBits(), 3f.toRawBits()))
    }

    @Test
    fun uniformsAreTrackedByName() {
        val cache = UniformCache()
        cache.hasChanged("a", UniformCache.KIND_INT, 1)

        assertTrue(cache.hasChanged("b", UniformCache.KIND_INT, 1))
        assertFalse(cache.hasChanged("a", UniformCache.KIND_INT, 1))
    }

    @Test
    fun signOfZeroIsAChange() {
        val cache = UniformCache()
        cache.hasChanged("a", UniformCache.KIND_FLOAT, 0f.toRawBits())

        assertTrue(cache.hasChanged("a", UniformCache.KIND_FLOAT, (-0f).toRawBits()))
    }

    @Test
    fun childIsComparedByIdentity() {
        val cache = UniformCache()
        val child = StringBuilder("child")
        cache.hasChildChanged("image", child)
        cache.hasChanges = false

        assertFalse(cache.hasChildChanged("image", child))
        assertFalse(cache.hasChanges)
        assertTrue(cache.hasChildChanged("image", StringBuilder("child")))
        assertTrue(cache.hasChanges)
    }
}
