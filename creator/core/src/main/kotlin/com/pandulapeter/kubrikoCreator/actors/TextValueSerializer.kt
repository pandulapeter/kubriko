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
 * Reads and writes a [TextValue] in JSON: a fixed one as a text in quotes.
 */
internal object TextValueSerializer : FixedOrPropertySerializer<TextValue, TextValue.Property>(
    valueClass = TextValue::class,
    summary = TextValue.SUMMARY,
    fixedName = "text",
    fixedKind = PrimitiveKind.STRING,
    propertyClass = TextValue.Property::class,
    propertySerializer = TextValue.Property.serializer(),
    noun = "a text value",
    fixedForm = "a text in quotes",
) {
    override fun encodeFixed(json: Json, value: TextValue) = JsonPrimitive((value as TextValue.Fixed).text)

    override fun decodeFixed(json: Json, element: JsonPrimitive) = if (element.isString) TextValue.Fixed(element.content) else throw notAValue(element)
}
