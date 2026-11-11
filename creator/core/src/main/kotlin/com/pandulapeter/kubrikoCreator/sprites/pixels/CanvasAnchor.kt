/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites.pixels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Where an image keeps its place when its canvas is resized ([PixelOperations.resizedCanvas]): one of the nine points of
 * a grid over it.
 */
@Serializable
public enum class CanvasAnchor {
    @SerialName("topLeft")
    TOP_LEFT,

    @SerialName("top")
    TOP,

    @SerialName("topRight")
    TOP_RIGHT,

    @SerialName("left")
    LEFT,

    @SerialName("center")
    CENTER,

    @SerialName("right")
    RIGHT,

    @SerialName("bottomLeft")
    BOTTOM_LEFT,

    @SerialName("bottom")
    BOTTOM,

    @SerialName("bottomRight")
    BOTTOM_RIGHT,
    ;

    /**
     * How far the image moves right when the canvas grows by [growth] pixels across.
     */
    public fun offsetAcross(growth: Int): Int = offsetOf(ordinal % 3, growth)

    /**
     * How far the image moves down when the canvas grows by [growth] pixels down.
     */
    public fun offsetDown(growth: Int): Int = offsetOf(ordinal / 3, growth)

    private fun offsetOf(place: Int, growth: Int) = when (place) {
        0 -> 0
        1 -> growth / 2
        else -> growth
    }
}
