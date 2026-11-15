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

import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

/**
 * Reads and writes a [SoundValue] in JSON: a fixed one as its bare path.
 */
internal object SoundValueSerializer : FixedOrPropertySerializer<SoundValue, SoundValue.Property>(
    valueClass = SoundValue::class,
    summary = SoundValue.SUMMARY,
    fixedName = "sound",
    fixedKind = PrimitiveKind.STRING,
    propertyClass = SoundValue.Property::class,
    propertySerializer = SoundValue.Property.serializer(),
    noun = "a sound value",
    fixedForm = "a sound file's path",
) {
    override fun encodeFixed(json: Json, value: SoundValue) = JsonPrimitive((value as SoundValue.Fixed).path)

    override fun decodeFixed(json: Json, element: JsonPrimitive) = if (element.isString) SoundValue.Fixed(element.content) else throw notAValue(element)
}
