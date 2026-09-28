/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TriangleBatchBookkeepingTest {

    @Test
    fun vertexIndicesRestartAfterReset() {
        val batch = TriangleBatch()
        assertTrue(batch.isEmpty)
        assertEquals(listOf(0, 1, 2), List(3) { batch.addVertex(it.toFloat(), 0f, 0) })
        assertFalse(batch.isEmpty)
        val generation = batch.generation
        batch.reset()
        assertEquals(generation + 1, batch.generation)
        assertTrue(batch.isEmpty)
        assertEquals(0, batch.addVertex(0f, 0f, 0))
    }

    @Test
    fun willOverflowAtTheBoundary() {
        val batch = TriangleBatch()
        repeat(10) { batch.addVertex(0f, 0f, 0) }
        assertFalse(batch.willOverflow(TriangleBatch.MAX_INDEXED_VERTICES - 10))
        assertTrue(batch.willOverflow(TriangleBatch.MAX_INDEXED_VERTICES - 9))
    }

    @Test
    fun addingPastTheLimitFailsUntilReset() {
        val batch = TriangleBatch()
        repeat(TriangleBatch.MAX_INDEXED_VERTICES) { batch.addVertex(0f, 0f, 0) }
        assertFailsWith<IllegalStateException> { batch.addVertex(0f, 0f, 0) }
        batch.reset()
        assertEquals(0, batch.addVertex(0f, 0f, 0))
    }

    @Test
    fun sharedCornersCountOnce() {
        val batch = TriangleBatch()
        val a = batch.addVertex(0f, 0f, 0)
        val b = batch.addVertex(1f, 0f, 0)
        val c = batch.addVertex(1f, 1f, 0)
        val d = batch.addVertex(0f, 1f, 0)
        batch.addTriangle(a, b, c)
        batch.addTriangle(a, c, d)
        assertFalse(batch.willOverflow(TriangleBatch.MAX_INDEXED_VERTICES - 4))
        assertTrue(batch.willOverflow(TriangleBatch.MAX_INDEXED_VERTICES - 3))
    }

    @Test
    fun degenerateLines() {
        val batch = TriangleBatch()
        batch.addLine(5f, 5f, 5.0001f, 5f, halfWidth = 1f, argbA = 0)
        assertTrue(batch.isEmpty)
        batch.addLine(Float.NaN, 0f, 1f, 1f, halfWidth = 1f, argbA = 0)
    }
}
