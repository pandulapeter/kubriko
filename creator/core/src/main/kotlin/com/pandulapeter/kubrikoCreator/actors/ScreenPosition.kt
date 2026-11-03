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

import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Where a Manager type's text is shown, with the Compose `Alignment` it becomes, and the horizontal one that lines up
 * the texts sharing it.
 */
@Serializable
@Description("A place on the screen.")
public enum class ScreenPosition(
    internal val alignment: String,
    internal val horizontalAlignment: String,
) {
    @SerialName("topLeft")
    TOP_LEFT("TopStart", "Start"),

    @SerialName("top")
    TOP("TopCenter", "CenterHorizontally"),

    @SerialName("topRight")
    TOP_RIGHT("TopEnd", "End"),

    @SerialName("left")
    LEFT("CenterStart", "Start"),

    @SerialName("center")
    CENTER("Center", "CenterHorizontally"),

    @SerialName("right")
    RIGHT("CenterEnd", "End"),

    @SerialName("bottomLeft")
    BOTTOM_LEFT("BottomStart", "Start"),

    @SerialName("bottom")
    BOTTOM("BottomCenter", "CenterHorizontally"),

    @SerialName("bottomRight")
    BOTTOM_RIGHT("BottomEnd", "End"),
}
