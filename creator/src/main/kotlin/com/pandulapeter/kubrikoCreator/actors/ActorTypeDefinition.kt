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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A block-built Actor type, as stored in `.kubriko/actors/<name>.json`. Kubriko Creator generates a Kotlin class from it.
 *
 * @param name The Actor type's name, which is also its Kotlin class name and its Scene type ID.
 * @param color The Actor's color as `0xAARRGGBB`, stored as `"#RRGGBB"` ([HexColorSerializer]).
 * @param width The default width of a newly placed Actor, in scene units.
 * @param height The default height of a newly placed Actor, in scene units.
 */
@Serializable
internal data class ActorTypeDefinition(
    @SerialName("name") val name: String,
    @SerialName("shape") val shape: ActorShape = ActorShape.CIRCLE,
    @SerialName("color") @Serializable(with = HexColorSerializer::class) val color: Long = 0xFF6060AA,
    @SerialName("width") val width: Float = 80f,
    @SerialName("height") val height: Float = 80f,
    @SerialName("events") val events: List<EventDefinition> = emptyList(),
)
