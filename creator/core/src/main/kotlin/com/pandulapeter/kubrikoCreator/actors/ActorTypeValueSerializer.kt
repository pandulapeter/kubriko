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
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Reads and writes an [ActorTypeValue] in JSON: a fixed type as its bare name, a Property's as an object whose `type` is
 * `property`. Its descriptor is a sealed one whose first variant is that bare name, which the tools' schemas and the
 * persisted shape read like any other sealed type.
 */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object ActorTypeValueSerializer : KSerializer<ActorTypeValue> {
    private const val SERIAL_NAME = "com.pandulapeter.kubrikoCreator.actors.ActorTypeValue"
    private const val TYPE_KEY = "type"
    private const val FIXED_NAME = "actorType"
    private const val PROPERTY_NAME = "property"

    override val descriptor: SerialDescriptor = buildSerialDescriptor(SERIAL_NAME, PolymorphicKind.SEALED) {
        annotations = listOf(SummarizedSchema(ActorTypeValue.SUMMARY))
        element("type", String.serializer().descriptor)
        element(
            "value",
            buildSerialDescriptor("$SERIAL_NAME<${ActorTypeValue::class.simpleName}>", SerialKind.CONTEXTUAL) {
                element(FIXED_NAME, PrimitiveSerialDescriptor("$SERIAL_NAME.$FIXED_NAME", PrimitiveKind.STRING))
                element(PROPERTY_NAME, ActorTypeValue.Property.serializer().descriptor)
            },
        )
    }

    override fun serialize(encoder: Encoder, value: ActorTypeValue) {
        val json = (encoder as? JsonEncoder)?.json ?: throw SerializationException("An Actor type value is only written as JSON.")
        encoder.encodeJsonElement(
            when (value) {
                is ActorTypeValue.Fixed -> JsonPrimitive(value.name)
                is ActorTypeValue.Property -> JsonObject(mapOf(TYPE_KEY to JsonPrimitive(PROPERTY_NAME)) + json.encodeToJsonElement(ActorTypeValue.Property.serializer(), value).jsonObject)
            },
        )
    }

    override fun deserialize(decoder: Decoder): ActorTypeValue {
        val json = (decoder as? JsonDecoder)?.json ?: throw SerializationException("An Actor type value is only read from JSON.")
        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> if (element.isString) ActorTypeValue.Fixed(element.content) else throw SerializationException(ERROR.format(element))
            is JsonObject -> when (val type = (element[TYPE_KEY] as? JsonPrimitive)?.contentOrNull) {
                PROPERTY_NAME -> json.decodeFromJsonElement(ActorTypeValue.Property.serializer(), JsonObject(element - TYPE_KEY))
                null -> throw SerializationException("An Actor type value written as an object needs a type: $PROPERTY_NAME.")
                else -> throw SerializationException("There is no Actor type value of the type '$type': it is an Actor type's name, or an object of the type $PROPERTY_NAME.")
            }
            else -> throw SerializationException(ERROR.format(element))
        }
    }

    private const val ERROR = "%s isn't an Actor type value: it is an Actor type's name, or an object of the type $PROPERTY_NAME."
}
