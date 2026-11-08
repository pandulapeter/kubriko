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
 * A rectangle filling the actor's body in one [color]. Its edges are smoothed like those of `drawRect`.
 *
 * @param color The fill color.
 */
class RectangleLook(color: Color = Color.White) : Look() {

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
