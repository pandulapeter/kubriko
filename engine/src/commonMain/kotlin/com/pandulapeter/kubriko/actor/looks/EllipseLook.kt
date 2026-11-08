/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.looks

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * An ellipse inscribed in the actor's body (a circle in a square one), in one [color]. It is built from as many
 * segments as its size on screen needs to look round, and its edge is smoothed like that of `drawOval`.
 *
 * @param color The fill color.
 */
class EllipseLook(color: Color = Color.White) : Look() {

    /** The fill color. Changing it rebuilds the actor's geometry on the next frame. */
    var color = color
        set(value) {
            if (field != value) {
                field = value
                argb = value.toArgb()
                markChanged()
            }
        }

    internal var argb = color.toArgb()
        private set
}
