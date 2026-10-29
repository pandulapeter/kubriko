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
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * A Timer's name, read from the number older projects saved too (Timer 3 becomes `timer3`).
 */
internal object TimerNameSerializer : KSerializer<String> {

    override val descriptor = PrimitiveSerialDescriptor("TimerName", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String {
        if (decoder !is JsonDecoder) return decoder.decodeString()
        val element = decoder.decodeJsonElement().jsonPrimitive
        return if (element.isString) element.content else element.intOrNull?.let(TimerNaming::ofNumber) ?: element.content
    }
}
