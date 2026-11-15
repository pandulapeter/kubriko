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
 * Reads and writes a [ColorValue] in JSON: a fixed color as [HexColorSerializer] writes and reads one, a Property's as an
 * object whose `type` is `property`. Its descriptor is a sealed one whose first variant is that
 * bare color, which the tools' schemas and the persisted shape read like any other sealed type.
 */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object ColorValueSerializer : KSerializer<ColorValue> {
    private const val SERIAL_NAME = "com.pandulapeter.kubrikoCreator.actors.ColorValue"
    private const val TYPE_KEY = "type"
    private const val FIXED_NAME = "color"
    private const val PROPERTY_NAME = "property"

    override val descriptor: SerialDescriptor = buildSerialDescriptor(SERIAL_NAME, PolymorphicKind.SEALED) {
        annotations = listOf(SummarizedSchema(ColorValue.SUMMARY))
        element("type", String.serializer().descriptor)
        element(
            "value",
            buildSerialDescriptor("$SERIAL_NAME<${ColorValue::class.simpleName}>", SerialKind.CONTEXTUAL) {
                element(FIXED_NAME, PrimitiveSerialDescriptor("$SERIAL_NAME.$FIXED_NAME", PrimitiveKind.STRING))
                element(PROPERTY_NAME, ColorValue.Property.serializer().descriptor)
            },
        )
    }

    override fun serialize(encoder: Encoder, value: ColorValue) {
        val json = (encoder as? JsonEncoder)?.json ?: throw SerializationException("A color value is only written as JSON.")
        encoder.encodeJsonElement(
            when (value) {
                is ColorValue.Fixed -> json.encodeToJsonElement(HexColorSerializer, value.argb)
                is ColorValue.Property -> JsonObject(mapOf(TYPE_KEY to JsonPrimitive(PROPERTY_NAME)) + json.encodeToJsonElement(ColorValue.Property.serializer(), value).jsonObject)
            },
        )
    }

    override fun deserialize(decoder: Decoder): ColorValue {
        val json = (decoder as? JsonDecoder)?.json ?: throw SerializationException("A color value is only read from JSON.")
        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> ColorValue.Fixed(json.decodeFromJsonElement(HexColorSerializer, element))
            is JsonObject -> when (val type = (element[TYPE_KEY] as? JsonPrimitive)?.contentOrNull) {
                PROPERTY_NAME -> json.decodeFromJsonElement(ColorValue.Property.serializer(), JsonObject(element - TYPE_KEY))
                null -> throw SerializationException("A color value written as an object needs a type: $PROPERTY_NAME.")
                else -> throw SerializationException("There is no color value of the type '$type': it is #RRGGBB, or an object of the type $PROPERTY_NAME.")
            }
            else -> throw SerializationException("$element isn't a color value: it is #RRGGBB, or an object of the type $PROPERTY_NAME.")
        }
    }
}
