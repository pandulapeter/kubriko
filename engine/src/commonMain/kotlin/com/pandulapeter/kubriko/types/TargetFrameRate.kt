/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.types

/**
 * Defines the target update frequency of the game loop.
 *
 * Unlike a frame divider, this expresses an absolute rate, so the resulting frame rate is
 * independent of the display's refresh rate: a 60 fps target runs every frame on a 60 Hz panel and
 * every other frame on a 120 Hz panel.
 */
sealed interface TargetFrameRate {

    /**
     * Updates occur on every display frame, so the game loop runs at the device's maximum refresh
     * rate. This is the default.
     */
    data object DisplayDefault : TargetFrameRate

    /**
     * Caps the update rate to at most [framesPerSecond] ticks per second. Targets higher than the
     * display's refresh rate simply run every frame.
     *
     * @param framesPerSecond The maximum number of updates per second. Must be greater than 0.
     */
    data class Limit(
        val framesPerSecond: Int,
    ) : TargetFrameRate {
        init {
            require(framesPerSecond > 0) { "framesPerSecond must be greater than 0." }
        }
    }

    /**
     * Updates occur on every [divisor]-th display frame, so the resulting rate is the device's
     * refresh rate divided by [divisor]. Unlike [Limit], this stays tied to the display: a divisor
     * of 2 yields 60 fps on a 120 Hz panel but 30 fps on a 60 Hz one. [DisplayDivider] with a
     * divisor of 1 is equivalent to [DisplayDefault].
     *
     * Because the rate is derived by counting actual display frames, [DisplayDivider] must run at the
     * panel's native refresh rate. Combining it with any mechanism that lowers the display's real
     * refresh rate — an OS panel-refresh hint, `Surface.setFrameRate`, or a system display setting —
     * **compounds instead of stacking additively**: the divider then counts an already-reduced rate
     * and drops the result a second time (a divisor of 2 while the panel is hinted from 120 Hz to
     * 60 Hz yields 30 fps, not the expected 60). Use [Limit] for a target that should also let the
     * panel step down, since it names an absolute rate the panel can settle on directly.
     *
     * @param divisor The number of display frames per update. Must be greater than 0.
     */
    data class DisplayDivider(
        val divisor: Int,
    ) : TargetFrameRate {
        init {
            require(divisor > 0) { "divisor must be greater than 0." }
        }
    }
}
