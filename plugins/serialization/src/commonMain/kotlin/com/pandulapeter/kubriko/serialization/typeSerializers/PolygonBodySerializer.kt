/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.serialization.typeSerializers

import com.pandulapeter.kubriko.actor.body.PolygonBody
import com.pandulapeter.kubriko.extensions.center
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

typealias SerializablePolygonBody = @Serializable(with = PolygonBodySerializer::class) PolygonBody

@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = PolygonBody::class)
object PolygonBodySerializer : KSerializer<PolygonBody> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("polygonBody") {
        element("vertices", ListSerializer(SceneOffsetSerializer).descriptor)
        element("position", SceneOffsetSerializer.descriptor)
        element("pivot", SceneOffsetSerializer.descriptor)
        element("scale", ScaleSerializer.descriptor)
        element("rotation", AngleRadiansSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: PolygonBody) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ListSerializer(SceneOffsetSerializer), value.vertices)
            encodeSerializableElement(descriptor, 1, SceneOffsetSerializer, value.position)
            encodeSerializableElement(descriptor, 2, SceneOffsetSerializer, value.pivot)
            encodeSerializableElement(descriptor, 3, ScaleSerializer, value.scale)
            encodeSerializableElement(descriptor, 4, AngleRadiansSerializer, value.rotation)
        }
    }

    override fun deserialize(decoder: Decoder): PolygonBody {
        return decoder.decodeStructure(descriptor) {
            var vertices = emptyList<SceneOffset>()
            var position = SceneOffset.Zero
            var pivot = vertices.center
            var scale = Scale.Unit
            var rotation = AngleRadians.Zero
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> vertices = decodeSerializableElement(descriptor, 0, ListSerializer(SceneOffsetSerializer), vertices)
                    1 -> position = decodeSerializableElement(descriptor, 1, SceneOffsetSerializer, position)
                    2 -> pivot = decodeSerializableElement(descriptor, 2, SceneOffsetSerializer, pivot)
                    3 -> scale = decodeSerializableElement(descriptor, 3, ScaleSerializer, scale)
                    4 -> rotation = decodeSerializableElement(descriptor, 4, AngleRadiansSerializer, rotation)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            PolygonBody(
                vertices = vertices,
                initialPosition = position,
                initialPivot = pivot,
                initialScale = scale,
                initialRotation = rotation,
            )
        }
    }
}