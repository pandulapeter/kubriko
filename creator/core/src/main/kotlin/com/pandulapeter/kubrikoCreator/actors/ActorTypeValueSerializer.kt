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
 * Reads and writes an [ActorTypeValue] in JSON: a fixed type as its bare name.
 */
internal object ActorTypeValueSerializer : FixedOrPropertySerializer<ActorTypeValue, ActorTypeValue.Property>(
    valueClass = ActorTypeValue::class,
    summary = ActorTypeValue.SUMMARY,
    fixedName = "actorType",
    fixedKind = PrimitiveKind.STRING,
    propertyClass = ActorTypeValue.Property::class,
    propertySerializer = ActorTypeValue.Property.serializer(),
    noun = "an Actor type value",
    fixedForm = "an Actor type's name",
) {
    override fun encodeFixed(json: Json, value: ActorTypeValue) = JsonPrimitive((value as ActorTypeValue.Fixed).name)

    override fun decodeFixed(json: Json, element: JsonPrimitive) = if (element.isString) ActorTypeValue.Fixed(element.content) else throw notAValue(element)
}
