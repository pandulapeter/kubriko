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

class NumberValueSerializerTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun aFixedNumberIsABareNumberAsTheFieldsWereBefore() {
        assertEquals("2.5", json.encodeToString(NumberValueSerializer, NumberValue.Fixed(2.5f)))
        assertEquals(NumberValue.Fixed(-3f), json.decodeFromString(NumberValueSerializer, "-3"))
        assertEquals(NumberValue.Fixed(1e-4f), json.decodeFromString(NumberValueSerializer, "1.0E-4"))
    }

    @Test
    fun aWholeFieldWritesItsNumberWithoutAFractionAndReadsOnlyWholeOnes() {
        assertEquals("3", json.encodeToString(WholeNumberValueSerializer, NumberValue.Fixed(3f)))
        assertEquals(NumberValue.Fixed(3f), json.decodeFromString(WholeNumberValueSerializer, "3"))
        assertEquals(NumberValue.Fixed(3f), json.decodeFromString(WholeNumberValueSerializer, "3.0"))
        assertFailsWith<SerializationException> { json.decodeFromString(WholeNumberValueSerializer, "2.5") }
    }

    @Test
    fun theOtherKindsAreObjectsNamingTheirType() {
        val random = NumberValue.Random(-1f, 4f, isWhole = true)
        assertEquals("""{"type":"random","from":-1.0,"to":4.0,"isWhole":true}""", json.encodeToString(NumberValueSerializer, random))
        assertEquals(random, json.decodeFromString(NumberValueSerializer, """{"from":-1,"type":"random","to":4,"isWhole":true}"""))
        assertEquals(NumberValue.Random(), json.decodeFromString(NumberValueSerializer, """{"type":"random"}"""))
        val property = NumberValue.Property("score", manager = "Game")
        assertEquals("""{"type":"property","property":"score","manager":"Game"}""", json.encodeToString(NumberValueSerializer, property))
        assertEquals(property, json.decodeFromString(NumberValueSerializer, """{"type":"property","property":"score","manager":"Game"}"""))
        assertEquals(property, json.decodeFromString(WholeNumberValueSerializer, json.encodeToString(WholeNumberValueSerializer, property)))
    }

    @Test
    fun anythingElseIsRefusedInPlainWords() {
        mapOf(
            "\"5\"" to "\"5\" isn't a number.",
            "null" to "null isn't a number.",
            "[1]" to "[1] isn't a number value: it is a number, or an object of the type random or property.",
            """{"from":1}""" to "A number value written as an object needs a type: random or property.",
            """{"type":"edge"}""" to "There is no number value of the type 'edge': it is a number, or an object of the type random or property.",
        ).forEach { (text, message) ->
            assertEquals(message, assertFailsWith<SerializationException>(text) { json.decodeFromString(NumberValueSerializer, text) }.message)
        }
    }

    @Test
    fun aBlockKeepsItsFieldsNamesWithAnyKindOfNumber() {
        val block = BlockDefinition.SetVelocity(x = NumberValue.Fixed(200f), y = NumberValue.Property("speed"))
        val text = json.encodeToString(BlockDefinition.serializer(), block)
        assertEquals("""{"type":"setVelocity","x":200.0,"y":{"type":"property","property":"speed","manager":null}}""", text)
        assertEquals(block, json.decodeFromString(BlockDefinition.serializer(), text))
        assertEquals(BlockDefinition.Repeat(NumberValue.Fixed(4f)), json.decodeFromString(BlockDefinition.serializer(), """{"type":"repeat","times":4}"""))
    }
}
