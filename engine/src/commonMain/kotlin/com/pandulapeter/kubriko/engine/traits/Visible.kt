package com.pandulapeter.kubriko.engine.traits

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.engine.implementation.extensions.deg
import com.pandulapeter.kubriko.engine.types.AngleDegrees
import com.pandulapeter.kubriko.engine.types.Scale
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import com.pandulapeter.kubriko.engine.types.WorldSize

interface Visible : Positionable {
    val rotation: AngleDegrees get() = 0f.deg
    val drawingOrder: Float get() = 0f

    fun draw(scope: DrawScope)
}