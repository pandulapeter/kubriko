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
 * Where a Manager type's text is shown, with the Compose `Alignment` it becomes.
 */
@Serializable
@Description("A place on the screen.")
internal enum class ScreenPosition(
    val alignment: String,
) {
    @SerialName("topLeft")
    TOP_LEFT("TopStart"),

    @SerialName("top")
    TOP("TopCenter"),

    @SerialName("topRight")
    TOP_RIGHT("TopEnd"),

    @SerialName("left")
    LEFT("CenterStart"),

    @SerialName("center")
    CENTER("Center"),

    @SerialName("right")
    RIGHT("CenterEnd"),

    @SerialName("bottomLeft")
    BOTTOM_LEFT("BottomStart"),

    @SerialName("bottom")
    BOTTOM("BottomCenter"),

    @SerialName("bottomRight")
    BOTTOM_RIGHT("BottomEnd"),
}
