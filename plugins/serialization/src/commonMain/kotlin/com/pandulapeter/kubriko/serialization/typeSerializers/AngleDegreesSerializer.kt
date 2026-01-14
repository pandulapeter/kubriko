/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.serialization.typeSerializers

import com.pandulapeter.kubriko.helpers.extensions.deg
import com.pandulapeter.kubriko.types.AngleDegrees
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SerializableAngleDegrees = @Serializable(with = AngleDegreesSerializer::class) AngleDegrees

@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = AngleDegrees::class)
object AngleDegreesSerializer : KSerializer<AngleDegrees> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("angleDegrees", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: AngleDegrees) {
        encoder.encodeFloat(value.normalized)
    }

    override fun deserialize(decoder: Decoder): AngleDegrees {
        return decoder.decodeFloat().deg
    }
}