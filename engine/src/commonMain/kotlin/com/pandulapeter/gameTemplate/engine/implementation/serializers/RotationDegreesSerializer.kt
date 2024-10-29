package com.pandulapeter.gameTemplate.engine.implementation.serializers

import com.pandulapeter.gameTemplate.engine.types.RotationDegrees
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SerializableRotationDegrees = @Serializable(with = RotationDegreesSerializer::class) RotationDegrees

@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = RotationDegrees::class)
object RotationDegreesSerializer : KSerializer<RotationDegrees> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("rotationDegrees", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: RotationDegrees) {
        encoder.encodeFloat(value.normalized)
    }

    override fun deserialize(decoder: Decoder): RotationDegrees {
        return RotationDegrees(decoder.decodeFloat())
    }
}