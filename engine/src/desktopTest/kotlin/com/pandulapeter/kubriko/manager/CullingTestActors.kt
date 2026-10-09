/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

/**
 * A 10 × 10 [Visible] box whose top-left corner sits at ([x], [y]).
 */
internal class TestVisible(
    x: Float,
    y: Float,
    override var drawingOrder: Float = 0f,
    override var layerIndex: Int? = 0,
    override val isAlwaysVisible: Boolean = false,
) : Visible {
    override val body = BoxBody(
        initialPosition = SceneOffset(x.sceneUnit, y.sceneUnit),
        initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit),
        initialPivot = SceneOffset.Zero,
    )

    fun moveTo(x: Float, y: Float) {
        body.position = SceneOffset(x.sceneUnit, y.sceneUnit)
    }

    override fun DrawScope.draw() = Unit
}

/**
 * An [Overlay] that draws nothing.
 */
internal class TestOverlay(
    override val overlayDrawingOrder: Float = 0f,
    override val layerIndex: Int? = 0,
) : Overlay {
    override fun DrawScope.drawToViewport() = Unit
}

/**
 * A [CountingActor] with a point body at ([x], [y]).
 */
internal class PositionedCountingActor(
    x: Float,
    y: Float,
    override val isAlwaysActive: Boolean = false,
) : CountingActor(), Positionable {
    override val body = PointBody(initialPosition = SceneOffset(x.sceneUnit, y.sceneUnit))
}
