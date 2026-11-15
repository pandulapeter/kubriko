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

class ActorTypeValueSerializerTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun aFixedTypeIsItsBareNameAsTheFieldsWereBefore() {
        assertEquals("\"Ball\"", json.encodeToString(ActorTypeValueSerializer, ActorTypeValue.Fixed("Ball")))
        assertEquals(ActorTypeValue.Fixed(""), json.decodeFromString(ActorTypeValueSerializer, "\"\""))
    }

    @Test
    fun aPropertysTypeIsAnObjectNamingItsType() {
        val property = ActorTypeValue.Property("enemyType", manager = "Game")
        assertEquals("""{"type":"property","property":"enemyType","manager":"Game"}""", json.encodeToString(ActorTypeValueSerializer, property))
        assertEquals(property, json.decodeFromString(ActorTypeValueSerializer, """{"property":"enemyType","manager":"Game","type":"property"}"""))
    }

    @Test
    fun anythingElseIsRefused() {
        listOf("3", "true", "{\"property\":\"x\"}", "{\"type\":\"random\"}", "[]").forEach { text ->
            assertFailsWith<SerializationException>(text) { json.decodeFromString(ActorTypeValueSerializer, text) }
        }
    }
}
