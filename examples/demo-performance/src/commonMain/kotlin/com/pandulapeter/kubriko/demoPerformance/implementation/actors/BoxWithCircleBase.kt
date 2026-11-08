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

import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.types.SceneUnit

/** The state and behavior every [BoxWithCircle] shares, plain or batched; only drawing differs between the two. */
internal abstract class BoxWithCircleBase(state: BoxWithCircle.State) : Dynamic, Editable<BoxWithCircle> {
    override val body = state.body

    @set:Exposed(name = "isRotatingClockwise")
    var isRotatingClockwise = state.isRotatingClockwise

    @set:Exposed(name = "boxColor")
    var boxColor: Color = state.boxColor
        set(value) {
            field = value
            onAppearanceChanged()
        }

    @set:Exposed(name = "circleRadius")
    var circleRadius: SceneUnit = state.circleRadius
        set(value) {
            field = value
            onAppearanceChanged()
        }

    @set:Exposed(name = "circleColor")
    var circleColor: Color = state.circleColor
        set(value) {
            field = value
            onAppearanceChanged()
        }

    override fun update(deltaTimeInMilliseconds: Int) {
        body.rotation += 0.001f.rad * deltaTimeInMilliseconds * (if (isRotatingClockwise) 1 else -1)
    }

    /** Called whenever a property the actor is drawn from changes. */
    protected abstract fun onAppearanceChanged()

    override fun save() = BoxWithCircle.State(
        body = body,
        boxColor = boxColor,
        circleColor = circleColor,
        circleRadius = circleRadius,
        isRotatingClockwise = isRotatingClockwise,
    )
}
