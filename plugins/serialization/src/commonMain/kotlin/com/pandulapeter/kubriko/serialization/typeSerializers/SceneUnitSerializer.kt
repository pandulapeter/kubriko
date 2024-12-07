package com.pandulapeter.kubriko.serialization.typeSerializers

import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SerializableSceneUnit = @Serializable(with = SceneUnitSerializer::class) SceneUnit

@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = SceneUnit::class)
object SceneUnitSerializer : KSerializer<SceneUnit> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("sceneUnit", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: SceneUnit) {
        encoder.encodeFloat(value.raw)
    }

    override fun deserialize(decoder: Decoder): SceneUnit {
        return decoder.decodeFloat().sceneUnit
    }
}