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

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A [Color] encoded with [ColorSerializer]: use it as a property type in a `@Serializable` state class.
 */
typealias SerializableColor = @Serializable(with = ColorSerializer::class) Color

/**
 * Encodes a [Color] as a long primitive holding its packed value, with the descriptor name `"color"`.
 */
@Suppress("EXTERNAL_SERIALIZER_USELESS")
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Color::class)
object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("color", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeLong(value.value.toLong())
    }

    override fun deserialize(decoder: Decoder): Color {
        return Color(decoder.decodeLong().toULong())
    }
}