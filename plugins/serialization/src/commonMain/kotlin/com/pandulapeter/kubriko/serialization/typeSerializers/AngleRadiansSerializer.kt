package com.pandulapeter.kubriko.serialization.typeSerializers

import com.pandulapeter.kubriko.extensions.rad
import com.pandulapeter.kubriko.types.AngleRadians
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SerializableAngleRadians = @Serializable(with = AngleRadiansSerializer::class) AngleRadians

@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = AngleRadians::class)
object AngleRadiansSerializer : KSerializer<AngleRadians> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("angleRadians", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: AngleRadians) {
        encoder.encodeFloat(value.normalized)
    }

    override fun deserialize(decoder: Decoder): AngleRadians {
        return decoder.decodeFloat().rad
    }
}