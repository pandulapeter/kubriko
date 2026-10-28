/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Stores a `0xAARRGGBB` color as `"#RRGGBB"`, or `"#AARRGGBB"` when it isn't opaque, so that definitions stay readable.
 * Reads the plain numbers earlier versions wrote too.
 */
internal object HexColorSerializer : KSerializer<Long> {

    override val descriptor = PrimitiveSerialDescriptor("HexColor", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeString(ColorHex.format(value))

    override fun deserialize(decoder: Decoder): Long {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() as? JsonPrimitive
        val text = element?.content ?: decoder.decodeString()
        return element?.takeUnless { it.isString }?.longOrNull ?: text.takeIf { it.startsWith("#") }?.let(ColorHex::parse) ?: throw SerializationException("Not a color: $text")
    }
}
