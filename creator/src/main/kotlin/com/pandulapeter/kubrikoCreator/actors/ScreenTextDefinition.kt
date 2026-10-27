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

import com.pandulapeter.kubrikoCreator.commands.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A text a Manager type shows on top of the game, drawn by the generated Manager's `Composable`.
 *
 * @param color The text's color as `0xAARRGGBB`, stored as `"#RRGGBB"` ([HexColorSerializer]).
 */
@Serializable
@Description("A text shown on top of the game, in a corner, along an edge, or in the middle of the screen.")
internal data class ScreenTextDefinition(
    @Description("The text. {name} is replaced by the value of the Manager's Property called name, and {Other.name} by a Property of the Manager type Other, as in \"Score: {score}\".")
    @SerialName("text") val text: String = "",
    @Description("Where on the screen it is shown.")
    @SerialName("position") val position: ScreenPosition = ScreenPosition.TOP_LEFT,
    @Description("The size of its letters.")
    @SerialName("size") val size: Float = 24f,
    @Description("Its color, as #RRGGBB, or #AARRGGBB when it isn't opaque.")
    @SerialName("color") @Serializable(with = HexColorSerializer::class) val color: Long = 0xFF202020,
)
