/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ListItemKeysTest {
    private var now = 0L
    private val keys = ListItemKeys<String>(clock = { now })

    @Test
    fun theFirstVersionGetsNewKeysThatDontCountAsAdded() {
        val first = keys.update(listOf("a", "b", "c"))
        assertEquals(3, first.toSet().size)
        assertTrue(first.none(keys::isRecentlyAdded))
        assertEquals(first, keys.update(listOf("a", "b", "c")))
    }

    @Test
    fun aMovedItemKeepsItsKey() {
        val (a, b, c, d) = keys.update(listOf("a", "b", "c", "d"))
        assertEquals(listOf(a, c, d, b), keys.update(listOf("a", "c", "d", "b")))
        assertEquals(listOf(b, a, c, d), keys.update(listOf("b", "a", "c", "d")))
    }

    @Test
    fun anEditedItemKeepsTheKeyOfItsPlace() {
        val (a, b, c) = keys.update(listOf("a", "b", "c"))
        assertEquals(listOf(a, b, c), keys.update(listOf("a", "B", "c")))
        assertEquals(listOf(a, b, c), keys.update(listOf("x", "y", "z")))
    }

    @Test
    fun anAddedItemGetsANewKeyThatCountsAsAddedForAWhile() {
        val (a, b) = keys.update(listOf("a", "b"))
        val next = keys.update(listOf("a", "new", "b"))
        assertEquals(a, next[0])
        assertEquals(b, next[2])
        assertNotEquals(a, next[1])
        assertNotEquals(b, next[1])
        assertTrue(keys.isRecentlyAdded(next[1]))
        assertFalse(keys.isRecentlyAdded(a))
        now += ListItemKeys.ADDED_DURATION_IN_NANOSECONDS
        assertFalse(keys.isRecentlyAdded(next[1]))
    }

    @Test
    fun removingKeepsTheOtherKeysAndAddingBackGivesANewOne() {
        val (a, b, c) = keys.update(listOf("a", "b", "c"))
        assertEquals(listOf(a, c), keys.update(listOf("a", "c")))
        val restored = keys.update(listOf("a", "b", "c"))
        assertEquals(listOf(a, c), listOf(restored[0], restored[2]))
        assertNotEquals(b, restored[1])
        assertTrue(keys.isRecentlyAdded(restored[1]))
    }

    @Test
    fun equalItemsKeepDistinctKeys() {
        val first = keys.update(listOf("stop", "stop"))
        val next = keys.update(listOf("stop", "stop", "stop"))
        assertEquals(first, next.take(2))
        assertEquals(3, next.toSet().size)
    }
}
