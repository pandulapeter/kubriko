/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import androidx.compose.ui.input.key.Key
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyChoiceTest {
    private val json = Json

    @Test
    fun everyKeyComposeKnowsCanBeChosenTheCommonOnesFirst() {
        assertEquals(composeKeys.size, KeyChoice.all.size)
        assertEquals(KeyChoice.all.size, KeyChoice.all.distinct().size)
        assertEquals(KeyChoice.common, KeyChoice.all.take(KeyChoice.common.size))
        assertTrue(KeyChoice.all.all { it.isKnown })
        assertTrue(KeyChoice("F12").isKnown)
        assertFalse(KeyChoice("Home").isKnown)
        assertFalse(KeyChoice("Hyper").isKnown)
    }

    @Test
    fun keysAreSortedLikeTheListSoGeneratedCodeKeepsItsOrder() {
        assertEquals(listOf(KeyChoice.LEFT, KeyChoice.SPACE, KeyChoice.D, KeyChoice("F1"), KeyChoice("Hyper")), listOf(KeyChoice("Hyper"), KeyChoice("F1"), KeyChoice.D, KeyChoice.SPACE, KeyChoice.LEFT).sorted())
    }

    @Test
    fun olderProjectsNamesAndAnyCaseAreRead() {
        assertEquals(KeyChoice.LEFT, json.decodeFromString<KeyChoice>("\"left\""))
        assertEquals(KeyChoice.SPACE, json.decodeFromString<KeyChoice>("\"space\""))
        assertEquals(KeyChoice.W, json.decodeFromString<KeyChoice>("\"w\""))
        assertEquals(KeyChoice("PageUp"), json.decodeFromString<KeyChoice>("\"pageup\""))
        assertEquals(KeyChoice("Hyper"), json.decodeFromString<KeyChoice>("\"Hyper\""))
        assertEquals("\"Spacebar\"", json.encodeToString(KeyChoice.SPACE))
        assertEquals(EventDefinition(EventType.KEY_PRESSED, KeyChoice.ENTER), json.decodeFromString<EventDefinition>("""{"type":"keyPressed","key":"enter"}"""))
    }

    @Test
    fun aPressedKeyIsFoundByItsComposeKey() {
        assertEquals("NumPad7", composeKeyNameOf(Key.NumPad7))
        assertEquals("ShiftRight", composeKeyNameOf(Key.ShiftRight))
        assertNull(composeKeyNameOf(Key.Unknown))
    }
}
