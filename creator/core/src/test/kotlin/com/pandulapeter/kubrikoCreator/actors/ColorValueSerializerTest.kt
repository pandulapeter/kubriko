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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ColorValueSerializerTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun aFixedColorIsABareHexCodeAsTheFieldsWereBeforeAndThePlainNumbersOfTheEarliestVersionsStillRead() {
        assertEquals("\"#20C060\"", json.encodeToString(ColorValueSerializer, ColorValue.Fixed(0xFF20C060)))
        assertEquals("\"#8020C060\"", json.encodeToString(ColorValueSerializer, ColorValue.Fixed(0x8020C060)))
        assertEquals(ColorValue.Fixed(0xFF20C060), json.decodeFromString(ColorValueSerializer, "\"#20C060\""))
        assertEquals(ColorValue.Fixed(0xFF20C060), json.decodeFromString(ColorValueSerializer, 0xFF20C060.toString()))
    }

    @Test
    fun aPropertysColorIsAnObjectNamingItsType() {
        val property = ColorValue.Property("tint", manager = "Game")
        assertEquals("""{"type":"property","property":"tint","manager":"Game"}""", json.encodeToString(ColorValueSerializer, property))
        assertEquals(property, json.decodeFromString(ColorValueSerializer, """{"manager":"Game","type":"property","property":"tint"}"""))
    }

    @Test
    fun anythingElseIsRefusedNamingWhatAColorValueIs() {
        listOf("\"red\"", "{\"property\":\"tint\"}", "{\"type\":\"random\"}", "[]", "true").forEach { text ->
            assertFailsWith<SerializationException>(text) { json.decodeFromString(ColorValueSerializer, text) }
        }
    }
}
