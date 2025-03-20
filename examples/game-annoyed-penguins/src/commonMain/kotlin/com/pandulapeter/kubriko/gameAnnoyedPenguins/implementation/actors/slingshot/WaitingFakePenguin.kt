/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.slingshot

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.BlinkingPenguin
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.max
import kotlin.math.min

/**
 * This penguin waits at the bottom of the slingshot.
 */
internal class WaitingFakePenguin(
    initialPosition: SceneOffset,
) : BlinkingPenguin(initialPosition), Unique, Dynamic {

    override val drawingOrder = -3f
    private var scale = 1f
        set(value) {
            if (field != value) {
                field = min(1f, max(0f, value))
                body.scale = Scale.Unit * field
            }
        }

    override fun onAdded(kubriko: Kubriko) {
        super<BlinkingPenguin>.onAdded(kubriko)
        scale = 0f
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        super.update(deltaTimeInMilliseconds)
        if (scale < 1f) {
            scale += 0.005f * deltaTimeInMilliseconds
        }
    }

    fun reset() {
        scale = 0f
    }
}