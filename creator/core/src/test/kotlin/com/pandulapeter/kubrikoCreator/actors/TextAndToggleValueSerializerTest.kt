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

class TextAndToggleValueSerializerTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun fixedValuesStayBareAsTheFieldsWereBefore() {
        assertEquals("\"Game \\\"over\\\"\"", json.encodeToString(TextValueSerializer, TextValue.Fixed("Game \"over\"")))
        assertEquals(TextValue.Fixed(""), json.decodeFromString(TextValueSerializer, "\"\""))
        assertEquals("false", json.encodeToString(ToggleValueSerializer, ToggleValue.Fixed(false)))
        assertEquals(ToggleValue.Fixed(true), json.decodeFromString(ToggleValueSerializer, "true"))
    }

    @Test
    fun aPropertysValueIsAnObjectNamingItsType() {
        assertEquals("""{"type":"property","property":"title","manager":"Game"}""", json.encodeToString(TextValueSerializer, TextValue.Property("title", manager = "Game")))
        assertEquals(ToggleValue.Property("isOver"), json.decodeFromString(ToggleValueSerializer, """{"type":"property","property":"isOver"}"""))
    }

    @Test
    fun anythingElseIsRefused() {
        listOf("3", "true", "{\"property\":\"x\"}", "[]").forEach { text ->
            assertFailsWith<SerializationException>(text) { json.decodeFromString(TextValueSerializer, text) }
        }
        listOf("3", "\"true\"", "\"on\"", "{\"type\":\"random\"}").forEach { text ->
            assertFailsWith<SerializationException>(text) { json.decodeFromString(ToggleValueSerializer, text) }
        }
    }
}
