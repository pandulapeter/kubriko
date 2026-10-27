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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Generates the JSON schema of a command's input from its serial descriptor, so the schema can't drift from the class
 * the input is decoded into. Descriptions come from [Description] annotations.
 */
@OptIn(ExperimentalSerializationApi::class)
internal object JsonSchema {

    /**
     * The discriminator property of a sealed type's subclasses, as kotlinx.serialization writes it.
     */
    const val TYPE_PROPERTY = "type"

    fun of(descriptor: SerialDescriptor): JsonObject = schema(descriptor, description = descriptor.description)

    private fun schema(descriptor: SerialDescriptor, description: String?): JsonObject = if (descriptor.isNullable) {
        JsonObject(nonNullSchema(descriptor, description).mapValues { (key, value) ->
            when (key) {
                "type" -> buildJsonArray { add(value); add("null") }
                "enum" -> buildJsonArray { value.jsonArray.forEach { add(it) }; add(JsonNull) }
                "oneOf" -> buildJsonArray { value.jsonArray.forEach { add(it) }; add(buildJsonObject { put("type", "null") }) }
                else -> value
            }
        })
    } else {
        nonNullSchema(descriptor, description)
    }

    private fun nonNullSchema(descriptor: SerialDescriptor, description: String?): JsonObject = buildJsonObject {
        when (val kind = descriptor.kind) {
            PrimitiveKind.STRING, PrimitiveKind.CHAR -> put("type", "string")
            PrimitiveKind.BOOLEAN -> put("type", "boolean")
            PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG -> put("type", "integer")
            PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> put("type", "number")
            SerialKind.ENUM -> {
                put("type", "string")
                putJsonArray("enum") { (0 until descriptor.elementsCount).forEach { add(descriptor.getElementName(it)) } }
            }
            StructureKind.LIST -> {
                put("type", "array")
                put("items", schema(descriptor.getElementDescriptor(0), descriptor.getElementDescriptor(0).description))
            }
            StructureKind.CLASS, StructureKind.OBJECT -> putObjectProperties(descriptor, discriminator = null)
            PolymorphicKind.SEALED -> put("oneOf", buildJsonArray {
                descriptor.getElementDescriptor(1).elementDescriptors.forEach { subclass ->
                    add(buildJsonObject {
                        putObjectProperties(subclass, discriminator = subclass.serialName)
                        subclass.description?.let { put("description", it) }
                    })
                }
            })
            else -> error("${descriptor.serialName} is a $kind, which command inputs don't support.")
        }
        val text = listOfNotNull(description, descriptor.enumValueDescriptions()).joinToString(" ").ifEmpty { null }
        text?.let { put("description", it) }
    }

    private fun JsonObjectBuilder.putObjectProperties(descriptor: SerialDescriptor, discriminator: String?) {
        put("type", "object")
        putJsonObject("properties") {
            discriminator?.let { putJsonObject(TYPE_PROPERTY) { put("const", it) } }
            (0 until descriptor.elementsCount).forEach { index ->
                put(descriptor.getElementName(index), schema(descriptor.getElementDescriptor(index), descriptor.elementDescription(index)))
            }
        }
        val required = listOfNotNull(discriminator?.let { TYPE_PROPERTY }) +
            (0 until descriptor.elementsCount).filterNot(descriptor::isElementOptional).map(descriptor::getElementName)
        if (required.isNotEmpty()) putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
        put("additionalProperties", false)
    }

    private fun SerialDescriptor.enumValueDescriptions() = if (kind == SerialKind.ENUM) {
        (0 until elementsCount).mapNotNull { index -> elementDescription(index)?.let { "${getElementName(index)}: $it" } }.joinToString(" ").ifEmpty { null }
    } else {
        null
    }

    private val SerialDescriptor.description get() = annotations.filterIsInstance<Description>().firstOrNull()?.text

    private fun SerialDescriptor.elementDescription(index: Int) = getElementAnnotations(index).filterIsInstance<Description>().firstOrNull()?.text
}
