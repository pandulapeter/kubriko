package com.pandulapeter.kubriko.serialization.typeSerializers

import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
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

typealias SerializableRectangleBody = @Serializable(with = RectangleBodySerializer::class) RectangleBody

@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = RectangleBody::class)
object RectangleBodySerializer : KSerializer<RectangleBody> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("rectangleBody") {
        element("position", SceneOffsetSerializer.descriptor)
        element("size", SceneSizeSerializer.descriptor)
        element("pivot", SceneOffsetSerializer.descriptor)
        element("scale", ScaleSerializer.descriptor)
        element("rotation", AngleRadiansSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: RectangleBody) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, SceneOffsetSerializer, value.position)
            encodeSerializableElement(descriptor, 1, SceneSizeSerializer, value.size)
            encodeSerializableElement(descriptor, 2, SceneOffsetSerializer, value.pivot)
            encodeSerializableElement(descriptor, 3, ScaleSerializer, value.scale)
            encodeSerializableElement(descriptor, 4, AngleRadiansSerializer, value.rotation)
        }
    }

    override fun deserialize(decoder: Decoder): RectangleBody {
        return decoder.decodeStructure(descriptor) {
            var position: SceneOffset = SceneOffset.Zero
            var size: SceneSize = SceneSize.Zero
            var pivot: SceneOffset = size.center
            var scale: Scale = Scale.Unit
            var rotation: AngleRadians = AngleRadians.Zero
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> position = decodeSerializableElement(descriptor, 0, SceneOffsetSerializer, position)
                    1 -> size = decodeSerializableElement(descriptor, 1, SceneSizeSerializer, size)
                    2 -> pivot = decodeSerializableElement(descriptor, 2, SceneOffsetSerializer, pivot)
                    3 -> scale = decodeSerializableElement(descriptor, 3, ScaleSerializer, scale)
                    4 -> rotation = decodeSerializableElement(descriptor, 4, AngleRadiansSerializer, rotation)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            RectangleBody(
                initialPosition = position,
                initialSize = size,
                initialPivot = pivot,
                initialScale = scale,
                initialRotation = rotation,
            )
        }
    }
}