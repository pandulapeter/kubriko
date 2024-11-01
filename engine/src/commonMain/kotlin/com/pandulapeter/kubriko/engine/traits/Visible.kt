package com.pandulapeter.kubriko.engine.traits

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.engine.types.AngleRadians

interface Visible : Positionable {
    val rotation: AngleRadians get() = AngleRadians.Zero
    val drawingOrder: Float get() = 0f

    fun draw(scope: DrawScope)
}