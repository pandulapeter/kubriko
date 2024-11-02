package com.pandulapeter.kubriko.engine.traits

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.engine.types.AngleRadians
import com.pandulapeter.kubriko.engine.types.ScenePixel

/**
 * Actors that want to be drawn on the screen should implement this interface.
 * A [Visible] actor must also be [Positionable].
 */
interface Visible : Positionable {

    /**
     * The angle of the rotation transformation that will be applied to the  drawing scope.
     */
    val rotation: AngleRadians get() = AngleRadians.Zero

    /**
     * This number will be used to determine the order of executing the [draw] function relative to other Actors.
     * Actors with smaller [drawingOrder]-s get drawn later (on top).
     */
    val drawingOrder: Float get() = 0f

    /**
     * Implement this function to draw the Actor into the Scene using the [scope], that has already been positioned, scaled and rotated.
     * The units used within this drawing scope must always be raw values of [ScenePixel].
     */
    fun draw(scope: DrawScope)
}