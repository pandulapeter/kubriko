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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventOtherActorType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonSchemaTest {

    @Serializable
    @Description("A sample.")
    private data class Sample(
        @Description("A name.")
        @SerialName("name") val name: String,
        @Description("A count.")
        @SerialName("count") val count: Int = 1,
        @Description("A ratio.")
        @SerialName("ratio") val ratio: Float? = null,
        @Description("Some flags.")
        @SerialName("flags") val flags: List<Boolean> = emptyList(),
        @Description("A choice.")
        @SerialName("choice") val choice: Choice = Choice.FIRST,
    )

    @Serializable
    private enum class Choice {
        @Description("The first one.")
        @SerialName("first")
        FIRST,

        @SerialName("second")
        SECOND,
    }

    @Test
    fun classesBecomeClosedObjectsWhosePropertiesWithoutDefaultsAreRequired() {
        val expected = """
            {"type":"object","properties":{
            "name":{"type":"string","description":"A name."},
            "count":{"type":"integer","description":"A count."},
            "ratio":{"type":["number","null"],"description":"A ratio."},
            "flags":{"type":"array","items":{"type":"boolean"},"description":"Some flags."},
            "choice":{"type":"string","enum":["first","second"],"description":"A choice. first: The first one."}},
            "required":["name"],"additionalProperties":false,"description":"A sample."}
        """.trimIndent().replace("\n", "")
        assertEquals(Json.parseToJsonElement(expected), JsonSchema.of(Sample.serializer().descriptor))
    }

    @Test
    fun sealedTypesBecomeOneSchemaPerSubclassTellingThemApartByType() {
        val block = JsonSchema.of(AddBlock.Input.serializer().descriptor)["properties"]!!.jsonObject["block"]!!.jsonObject
        val options = block["oneOf"]!!.jsonArray.map { it.jsonObject }
        assertEquals(BlockKind.entries.size, options.size)
        val setVelocity = options.single { it["properties"]!!.jsonObject["type"]!!.jsonObject["const"]!!.jsonPrimitive.content == "setVelocity" }
        assertEquals(listOf("type"), setVelocity["required"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("type", "x", "y"), setVelocity["properties"]!!.jsonObject.keys.toList())
    }

    @Test
    fun nullablePropertiesAcceptNull() {
        val other = JsonSchema.of(SetEventOtherActorType.Input.serializer().descriptor)["properties"]!!.jsonObject["otherActorType"]!!.jsonObject
        assertEquals(Json.parseToJsonElement("""["string","null"]"""), other["type"])
        val key = JsonSchema.of(AddEvent.Input.serializer().descriptor)["properties"]!!.jsonObject["key"]!!.jsonObject
        assertEquals(Json.parseToJsonElement("""["string","null"]"""), key["type"])
    }

    @Test
    fun customSerializersKeepTheirOwnFormat() {
        val color = JsonSchema.of(ActorTypeDefinition.serializer().descriptor)["properties"]!!.jsonObject["color"]!!.jsonObject
        assertEquals("string", color["type"]!!.jsonPrimitive.content)
    }
}
