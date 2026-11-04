/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The input serializer of a command Kubriko Creator makes only itself, which no tool names and nothing decodes.
 */
internal class NotATool<T>(
    private val commandName: String,
) : KSerializer<T> {
    override val descriptor = buildClassSerialDescriptor(commandName)

    override fun serialize(encoder: Encoder, value: T) = throw SerializationException("$commandName isn't a tool.")

    override fun deserialize(decoder: Decoder): T = throw SerializationException("$commandName isn't a tool.")
}
