package com.pandulapeter.gameTemplate.engine.implementation.serializers

import com.pandulapeter.gameTemplate.engine.types.MapSize
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

typealias SerializableMapSize = @Serializable(with = MapSizeSerializer::class) MapSize

@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = MapSize::class)
object MapSizeSerializer : KSerializer<MapSize> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("mapSize") {
        element<Float>("width")
        element<Float>("height")
    }

    override fun serialize(encoder: Encoder, value: MapSize) {
        encoder.encodeStructure(descriptor) {
            encodeFloatElement(descriptor, 0, value.width)
            encodeFloatElement(descriptor, 1, value.height)
        }
    }

    override fun deserialize(decoder: Decoder): MapSize {
        return decoder.decodeStructure(descriptor) {
            var width = 0f
            var height = 0f
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> width = decodeFloatElement(descriptor, 0)
                    1 -> height = decodeFloatElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            MapSize(width, height)
        }
    }
}