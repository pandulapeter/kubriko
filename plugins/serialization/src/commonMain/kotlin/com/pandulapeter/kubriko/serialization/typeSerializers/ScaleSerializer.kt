/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.serialization.typeSerializers

import com.pandulapeter.kubriko.types.Scale
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

typealias SerializableScale = @Serializable(with = ScaleSerializer::class) Scale

@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Scale::class)
object ScaleSerializer : KSerializer<Scale> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("scale") {
        element<Float>("horizontal")
        element<Float>("vertical")
    }

    override fun serialize(encoder: Encoder, value: Scale) {
        encoder.encodeStructure(descriptor) {
            encodeFloatElement(descriptor, 0, value.horizontal)
            encodeFloatElement(descriptor, 1, value.vertical)
        }
    }

    override fun deserialize(decoder: Decoder): Scale {
        return decoder.decodeStructure(descriptor) {
            var horizontal = 0f
            var vertical = 0f
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> horizontal = decodeFloatElement(descriptor, 0)
                    1 -> vertical = decodeFloatElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            Scale(horizontal, vertical)
        }
    }
}