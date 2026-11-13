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
 * An edge of the Scene's bounds, or of the screen while the Scene has none, as the edge Event decides them.
 */
@Serializable
public enum class SceneEdge {
    @SerialName("left")
    LEFT,

    @SerialName("right")
    RIGHT,

    @SerialName("top")
    TOP,

    @SerialName("bottom")
    BOTTOM,
    ;

    /**
     * The edge across from this one.
     */
    public val opposite: SceneEdge
        get() = when (this) {
            LEFT -> RIGHT
            RIGHT -> LEFT
            TOP -> BOTTOM
            BOTTOM -> TOP
        }
}
