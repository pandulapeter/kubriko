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
 * Reads and writes a [ColorValue] in JSON: a fixed color as [HexColorSerializer] writes and reads one.
 */
internal object ColorValueSerializer : FixedOrPropertySerializer<ColorValue, ColorValue.Property>(
    valueClass = ColorValue::class,
    summary = ColorValue.SUMMARY,
    fixedName = "color",
    fixedKind = PrimitiveKind.STRING,
    propertyClass = ColorValue.Property::class,
    propertySerializer = ColorValue.Property.serializer(),
    noun = "a color value",
    fixedForm = "#RRGGBB",
) {
    override fun encodeFixed(json: Json, value: ColorValue) = json.encodeToJsonElement(HexColorSerializer, checkNotNull(value.fixedArgb))

    override fun decodeFixed(json: Json, element: JsonPrimitive) = ColorValue.Fixed(json.decodeFromJsonElement(HexColorSerializer, element))
}
