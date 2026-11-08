/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import androidx.compose.ui.graphics.toArgb
import com.pandulapeter.kubriko.actor.looks.CustomLook
import com.pandulapeter.kubriko.actor.traits.Batched
import com.pandulapeter.kubriko.helpers.TriangleBatch

/**
 * The batched counterpart of [MovingBox]: it describes its look instead of drawing it, so the engine draws it in a run with its neighbors.
 * Its geometry is rebuilt only when it moves, turns, or changes color, which for a moving box is every frame: what it
 * saves is the canvas operations, not the arithmetic.
 */
internal class BatchedMovingBox(
    state: MovingBox.State,
    shouldRandomizeScale: Boolean = true,
) : MovingBoxBase(state, shouldRandomizeScale), Batched {

    override val look = object : CustomLook() {
        override fun emit(batch: TriangleBatch, width: Float, height: Float, pixelsPerUnit: Float) =
            batch.addOutlinedBox(width, height, boxColor.toArgb(), pixelsPerUnit)
    }

    override fun onAppearanceChanged() = look.invalidate()
}
