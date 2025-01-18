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

import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

typealias SerializablePointBody = @Serializable(with = PointBodySerializer::class) PointBody

@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = PointBody::class)
object PointBodySerializer : KSerializer<PointBody> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("pointBody") {
        element("position", SceneOffsetSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: PointBody) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, SceneOffsetSerializer, value.position)
        }
    }

    override fun deserialize(decoder: Decoder): PointBody {
        return decoder.decodeStructure(descriptor) {
            var position = SceneOffset.Zero
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> position = decodeSerializableElement(descriptor, 0, SceneOffsetSerializer, position)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            PointBody(
                initialPosition = position,
            )
        }
    }
}