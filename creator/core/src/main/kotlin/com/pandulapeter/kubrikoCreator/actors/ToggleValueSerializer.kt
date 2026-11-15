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
import kotlinx.serialization.json.booleanOrNull

/**
 * Reads and writes a [ToggleValue] in JSON: a fixed one as true or false.
 */
internal object ToggleValueSerializer : FixedOrPropertySerializer<ToggleValue, ToggleValue.Property>(
    valueClass = ToggleValue::class,
    summary = ToggleValue.SUMMARY,
    fixedName = "toggle",
    fixedKind = PrimitiveKind.BOOLEAN,
    propertyClass = ToggleValue.Property::class,
    propertySerializer = ToggleValue.Property.serializer(),
    noun = "a toggle value",
    fixedForm = "true or false",
) {
    override fun encodeFixed(json: Json, value: ToggleValue) = JsonPrimitive((value as ToggleValue.Fixed).isOn)

    override fun decodeFixed(json: Json, element: JsonPrimitive) = element.takeUnless { it.isString }?.booleanOrNull?.let(ToggleValue::Fixed) ?: throw notAValue(element)
}
