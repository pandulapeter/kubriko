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
import kotlin.reflect.KClass

/**
 * Reads and writes a value a Block takes that is either fixed or a Property's ([ColorValue], [ActorTypeValue], and the
 * like) in JSON: a fixed one as a bare JSON primitive, as those fields were before they could be anything else, a
 * Property's as an object whose `type` is `property`. Its descriptor is a sealed one whose first variant is that bare
 * value, which the tools' schemas and the persisted shape read like any other sealed type.
 *
 * @param valueClass The value's sealed type, which names the descriptor.
 * @param summary How the tools' schemas summarize the value ([SummarizedSchema]).
 * @param fixedName The name of the fixed variant in the descriptor.
 * @param fixedKind The kind of JSON primitive a fixed value is.
 * @param noun What the value is called in errors, with its article: "a color value".
 * @param fixedForm How a fixed value is written, for errors: "#RRGGBB".
 */
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal abstract class FixedOrPropertySerializer<V : Any, P : V>(
    valueClass: KClass<V>,
    summary: String,
    fixedName: String,
    fixedKind: PrimitiveKind,
    private val propertyClass: KClass<P>,
    private val propertySerializer: KSerializer<P>,
    private val noun: String,
    private val fixedForm: String,
) : KSerializer<V> {

    override val descriptor: SerialDescriptor = checkNotNull(valueClass.qualifiedName).let { serialName ->
        buildSerialDescriptor(serialName, PolymorphicKind.SEALED) {
            annotations = listOf(SummarizedSchema(summary))
            element("type", String.serializer().descriptor)
            element(
                "value",
                buildSerialDescriptor("$serialName<${valueClass.simpleName}>", SerialKind.CONTEXTUAL) {
                    element(fixedName, PrimitiveSerialDescriptor("$serialName.$fixedName", fixedKind))
                    element(PROPERTY_NAME, propertySerializer.descriptor)
                },
            )
        }
    }

    /**
     * [value], which isn't a Property's, as the bare primitive it is written as.
     */
    protected abstract fun encodeFixed(json: Json, value: V): JsonElement

    /**
     * The fixed value [element] is, throwing [notAValue] if it is none.
     */
    protected abstract fun decodeFixed(json: Json, element: JsonPrimitive): V

    protected fun notAValue(element: JsonElement): SerializationException = SerializationException("$element isn't $noun: it is $fixedForm, or an object of the type $PROPERTY_NAME.")

    final override fun serialize(encoder: Encoder, value: V) {
        val json = (encoder as? JsonEncoder)?.json ?: throw SerializationException("${noun.capitalized()} is only written as JSON.")
        encoder.encodeJsonElement(
            if (propertyClass.isInstance(value)) {
                @Suppress("UNCHECKED_CAST")
                JsonObject(mapOf(TYPE_KEY to JsonPrimitive(PROPERTY_NAME)) + json.encodeToJsonElement(propertySerializer, value as P).jsonObject)
            } else {
                encodeFixed(json, value)
            },
        )
    }

    final override fun deserialize(decoder: Decoder): V {
        val json = (decoder as? JsonDecoder)?.json ?: throw SerializationException("${noun.capitalized()} is only read from JSON.")
        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> decodeFixed(json, element)
            is JsonObject -> when (val type = (element[TYPE_KEY] as? JsonPrimitive)?.contentOrNull) {
                PROPERTY_NAME -> json.decodeFromJsonElement(propertySerializer, JsonObject(element - TYPE_KEY))
                null -> throw SerializationException("${noun.capitalized()} written as an object needs a type: $PROPERTY_NAME.")
                else -> throw SerializationException("There is no ${noun.substringAfter(' ')} of the type '$type': it is $fixedForm, or an object of the type $PROPERTY_NAME.")
            }
            else -> throw notAValue(element)
        }
    }

    private fun String.capitalized() = replaceFirstChar { it.uppercaseChar() }

    private companion object {
        const val TYPE_KEY = "type"
        const val PROPERTY_NAME = "property"
    }
}
