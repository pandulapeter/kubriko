package com.pandulapeter.kubriko.actor.traits

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.ScenePixel

/**
 * [Actor]s that want to be drawn on the Scene should implement this interface.
 * A [Visible] actor must also be [Positionable].
 */
interface Visible : Positionable, CanvasAware, Actor {

    /**
     * The angle of the rotation transformation that will be applied to the  drawing scope.
     */
    val rotation: AngleRadians get() = AngleRadians.Zero

    /**
     * This number will be used to determine the order of executing the [draw] function relative to other [Visible] [Actor]s.
     * [Actor]s with smaller [drawingOrder]-s get drawn later (on top).
     */
    val drawingOrder: Float get() = 0f

    /**
     * Implement this function to draw the [Actor] into the Scene using the [scope], that has already been positioned, scaled and rotated.
     * The units used within this drawing scope must always be raw values of [ScenePixel].
     */
    fun DrawScope.draw()
}