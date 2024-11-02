package com.pandulapeter.kubriko.engine.implementation.serializers

import com.pandulapeter.kubriko.engine.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.engine.types.ScenePixel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SerializableScenePixel = @Serializable(with = ScenePixelSerializer::class) ScenePixel

@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = ScenePixel::class)
object ScenePixelSerializer : KSerializer<ScenePixel> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("scenePixel", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: ScenePixel) {
        encoder.encodeFloat(value.raw)
    }

    override fun deserialize(decoder: Decoder): ScenePixel {
        return decoder.decodeFloat().scenePixel
    }
}