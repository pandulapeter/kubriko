package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.implementation.extensions.deg
import com.pandulapeter.gameTemplate.engine.types.AngleDegrees
import com.pandulapeter.gameTemplate.engine.types.Scale
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import com.pandulapeter.gameTemplate.engine.types.WorldSize

interface Visible {
    val boundingBox: WorldSize
    val pivotOffset: WorldCoordinates get() = boundingBox.center
    var position: WorldCoordinates
    val scale: Scale get() = Scale.Unit
    val rotation: AngleDegrees get() = 0f.deg
    val drawingOrder: Float get() = 0f

    fun draw(scope: DrawScope)
}