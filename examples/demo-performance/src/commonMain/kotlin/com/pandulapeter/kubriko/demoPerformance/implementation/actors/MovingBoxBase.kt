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
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.random.Random

/**
 * The state and behavior every [MovingBox] shares, plain or batched; only drawing differs between the two. A box given a
 * random scale when added ([shouldRandomizeScale]) starts pulsing from it; one switched from the other kind keeps its own.
 */
internal abstract class MovingBoxBase(
    state: MovingBox.State,
    private val shouldRandomizeScale: Boolean,
) : Dynamic, Editable<MovingBox> {
    override val body = state.body

    @set:Exposed(name = "isRotatingClockwise")
    var isRotatingClockwise = state.isRotatingClockwise

    @set:Exposed(name = "boxColor")
    var boxColor: Color = state.boxColor
        set(value) {
            field = value
            onAppearanceChanged()
        }

    private var isGrowing = true

    override fun onAdded(kubriko: Kubriko) {
        if (shouldRandomizeScale) body.scale = ((5..16).random() / 10f).let { Scale(it, it) }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        body.rotation += 0.001f.rad * deltaTimeInMilliseconds * (if (isRotatingClockwise) 1 else -1) * Random.nextFloat()
        if (isGrowing) {
            body.scale += deltaTimeInMilliseconds * 0.001f
        } else {
            body.scale -= deltaTimeInMilliseconds * 0.001f
        }
        if (body.scale.horizontal >= 1.6f) {
            isGrowing = false
            body.scale = Scale(1.6f, 1.6f)
        } else if (body.scale.horizontal <= 0.5f) {
            isGrowing = true
            body.scale = Scale(0.5f, 0.5f)
        }
        body.position += SceneOffset(
            x = body.rotation.cos.sceneUnit,
            y = -body.rotation.sin.sceneUnit,
        ) * deltaTimeInMilliseconds * 0.2f
    }

    /** Called whenever a property the actor is drawn from changes. */
    protected abstract fun onAppearanceChanged()

    override fun save() = MovingBox.State(
        body = body,
        boxColor = boxColor,
        isRotatingClockwise = isRotatingClockwise,
    )
}
