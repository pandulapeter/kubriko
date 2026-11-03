/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InputProblemTest {

    private val json = Json { ignoreUnknownKeys = false }

    @Test
    fun aBlockOfAnUnknownTypeIsNamedWithThePropertyHoldingIt() =
        assertEquals("There is no type called 'fly' at 'block'.", problemOf("""{"actorType":"Ball","eventIndex":0,"block":{"type":"fly"}}"""))

    @Test
    fun aBlockWithoutATypeSaysSo() =
        assertEquals("A type is missing at 'block'.", problemOf("""{"actorType":"Ball","eventIndex":0,"block":{"x":1}}"""))

    @Test
    fun anUnknownPropertyIsNamed() =
        assertEquals("There is no property called 'speed'.", problemOf("""{"actorType":"Ball","eventIndex":0,"block":{"type":"setVelocity","speed":1}}"""))

    @Test
    fun aValueOfTheWrongKindIsNamedWithWhatItShouldBe() =
        assertEquals("\"fast\" at 'x' isn't a float.", problemOf("""{"actorType":"Ball","eventIndex":0,"block":{"type":"setVelocity","x":"fast"}}"""))

    @Test
    fun missingPropertiesAreListed() {
        assertEquals("Missing: block.", problemOf("""{"actorType":"Ball","eventIndex":0}"""))
        assertEquals("Missing: actorType, block.", problemOf("""{"eventIndex":0}"""))
    }

    @Test
    fun anythingElseKeepsOnlyItsFirstLineWithoutTheOffset() =
        assertEquals("Something broke", InputProblem.describe("Unexpected JSON token at offset 12: Something broke\nJSON input: {}"))

    private fun problemOf(input: String) = InputProblem.describe(
        assertFailsWith<SerializationException> { json.decodeFromJsonElement(AddBlock.Input.serializer(), json.parseToJsonElement(input)) }.message,
    )
}
