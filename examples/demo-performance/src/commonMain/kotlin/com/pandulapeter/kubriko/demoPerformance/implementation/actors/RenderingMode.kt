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

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Batched

/** Whether this actor is a box, which [inRenderingMode] can switch between plain and batched. */
internal val Actor.isBox get() = this is MovingBoxBase || this is BoxWithCircleBase

/**
 * This actor drawn the other way, plain or [Batched] as [isBatched] says, in the same state, its scale included: itself
 * when it already is, and anything that is not a box unchanged.
 */
internal fun Actor.inRenderingMode(isBatched: Boolean): Actor = when {
    this is Batched == isBatched -> this
    this is MovingBoxBase -> if (isBatched) BatchedMovingBox(save(), shouldRandomizeScale = false) else MovingBox(save(), shouldRandomizeScale = false)
    this is BoxWithCircleBase -> if (isBatched) BatchedBoxWithCircle(save()) else BoxWithCircle(save())
    else -> this
}
