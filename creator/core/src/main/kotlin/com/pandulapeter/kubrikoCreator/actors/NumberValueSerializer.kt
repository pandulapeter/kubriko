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

import com.pandulapeter.kubrikoCreator.schema.SummarizedSchema
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.math.roundToLong

/**
 * Reads and writes a [NumberValue] in JSON: a fixed number as a bare number, the others as an object whose `type` names
 * their kind. Its descriptor is a sealed one whose first variant is that bare number, which the tools' schemas and the
 * persisted shape read like any other sealed type.
 */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal abstract class NumberValueFormat(
    serialName: String,
    private val isWhole: Boolean,
) : KSerializer<NumberValue> {

    override val descriptor: SerialDescriptor = buildSerialDescriptor(serialName, PolymorphicKind.SEALED) {
        annotations = listOf(SummarizedSchema(NumberValue.SUMMARY))
        element("type", String.serializer().descriptor)
        element(
            "value",
            buildSerialDescriptor("$serialName<${NumberValue::class.simpleName}>", SerialKind.CONTEXTUAL) {
                element(FIXED_NAME, PrimitiveSerialDescriptor("$serialName.$FIXED_NAME", if (isWhole) PrimitiveKind.INT else PrimitiveKind.FLOAT))
                element(RANDOM_NAME, NumberValue.Random.serializer().descriptor)
                element(PROPERTY_NAME, NumberValue.Property.serializer().descriptor)
                element(EDGE_NAME, NumberValue.Edge.serializer().descriptor)
                element(BETWEEN_EDGES_NAME, NumberValue.BetweenEdges.serializer().descriptor)
            },
        )
    }

    override fun serialize(encoder: Encoder, value: NumberValue) {
        val json = (encoder as? JsonEncoder)?.json ?: throw SerializationException("A number value is only written as JSON.")
        encoder.encodeJsonElement(
            when (value) {
                is NumberValue.Fixed -> when {
                    !value.value.isFinite() && !json.configuration.allowSpecialFloatingPointValues -> throw SerializationException("${value.value} isn't a finite number, which JSON can't hold.")
                    isWhole -> JsonPrimitive(value.value.roundToLong())
                    else -> JsonPrimitive(value.value)
                }
                is NumberValue.Random -> withType(RANDOM_NAME, json.encodeToJsonElement(NumberValue.Random.serializer(), value))
                is NumberValue.Property -> withType(PROPERTY_NAME, json.encodeToJsonElement(NumberValue.Property.serializer(), value))
                is NumberValue.Edge -> withType(EDGE_NAME, json.encodeToJsonElement(NumberValue.Edge.serializer(), value))
                is NumberValue.BetweenEdges -> withType(BETWEEN_EDGES_NAME, json.encodeToJsonElement(NumberValue.BetweenEdges.serializer(), value))
            },
        )
    }

    override fun deserialize(decoder: Decoder): NumberValue {
        val json = (decoder as? JsonDecoder)?.json ?: throw SerializationException("A number value is only read from JSON.")
        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> fixedOf(element, json)
            is JsonObject -> {
                val fields = JsonObject(element - TYPE_KEY)
                when (val type = (element[TYPE_KEY] as? JsonPrimitive)?.contentOrNull) {
                    RANDOM_NAME -> json.decodeFromJsonElement(NumberValue.Random.serializer(), fields)
                    PROPERTY_NAME -> json.decodeFromJsonElement(NumberValue.Property.serializer(), fields)
                    EDGE_NAME -> json.decodeFromJsonElement(NumberValue.Edge.serializer(), fields)
                    BETWEEN_EDGES_NAME -> json.decodeFromJsonElement(NumberValue.BetweenEdges.serializer(), fields)
                    null -> throw SerializationException("A number value written as an object needs a type: $OBJECT_TYPES.")
                    else -> throw SerializationException("There is no number value of the type '$type': it is a number, or an object of the type $OBJECT_TYPES.")
                }
            }
            else -> throw SerializationException("$element isn't a number value: it is a number, or an object of the type $OBJECT_TYPES.")
        }
    }

    private fun fixedOf(element: JsonPrimitive, json: Json): NumberValue.Fixed {
        val number = element.takeUnless { it.isString }?.contentOrNull?.toFloatOrNull()
            ?.takeIf { it.isFinite() || json.configuration.allowSpecialFloatingPointValues }
            ?: throw SerializationException("$element isn't a number.")
        if (isWhole && number != number.toLong().toFloat()) throw SerializationException("${element.content} isn't a whole number.")
        return NumberValue.Fixed(number)
    }

    private fun withType(type: String, fields: JsonElement) = JsonObject(mapOf(TYPE_KEY to JsonPrimitive(type)) + fields.jsonObject)

    private companion object {
        const val TYPE_KEY = "type"
        const val FIXED_NAME = "number"
        const val RANDOM_NAME = "random"
        const val PROPERTY_NAME = "property"
        const val EDGE_NAME = "edge"
        const val BETWEEN_EDGES_NAME = "betweenEdges"
        const val OBJECT_TYPES = "$RANDOM_NAME, $PROPERTY_NAME, $EDGE_NAME, or $BETWEEN_EDGES_NAME"
    }
}

/**
 * How every number field is written, unless it only takes whole numbers.
 */
internal object NumberValueSerializer : NumberValueFormat("com.pandulapeter.kubrikoCreator.actors.NumberValue", isWhole = false)

/**
 * How a field that only takes whole numbers is written: its fixed numbers without a fraction, as the field was written
 * while it was an `Int`, and only whole numbers are read.
 */
internal object WholeNumberValueSerializer : NumberValueFormat("com.pandulapeter.kubrikoCreator.actors.WholeNumberValue", isWhole = true)
