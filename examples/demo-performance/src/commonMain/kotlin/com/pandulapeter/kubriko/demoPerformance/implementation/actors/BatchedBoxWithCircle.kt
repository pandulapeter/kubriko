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
 * The batched counterpart of [BoxWithCircle]: it describes its look instead of drawing it, so the engine draws it in a run with its neighbors.
 * It only turns, so its geometry is rebuilt as it does, and not at all while it sleeps off screen.
 */
internal class BatchedBoxWithCircle(state: BoxWithCircle.State) : BoxWithCircleBase(state), Batched {

    override val look = object : CustomLook() {
        override fun emit(batch: TriangleBatch, width: Float, height: Float, pixelsPerUnit: Float) {
            batch.addOutlinedBox(width, height, boxColor.toArgb(), pixelsPerUnit)
            batch.addOutlinedCircle(width / 2f, height / 2f, circleRadius.raw, circleColor.toArgb(), pixelsPerUnit)
        }
    }

    override fun onAppearanceChanged() = look.invalidate()
}
