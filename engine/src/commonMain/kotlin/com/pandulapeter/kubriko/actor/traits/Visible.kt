package com.pandulapeter.kubriko.actor.traits

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.types.ScenePixel

/**
 * [Actor]s that want to be drawn on the Scene should implement this interface.
 * A [Visible] actor must also be [Positionable].
 */
interface Visible : Positionable, LayerAware {

    override val body: RectangleBody

    /**
     * This number will be used to determine the order of executing the [draw] function relative to other [Visible] [Actor]s.
     * [Actor]s with smaller [drawingOrder]-s get drawn later (on top).
     */
    val drawingOrder: Float get() = 0f

    val isVisible: Boolean get() = true

    /**
     * Implement this function to draw the [Actor] into the Scene using the [scope], that has already been positioned, scaled and rotated.
     * The units used within this drawing scope must always be raw values of [ScenePixel].
     */
    fun DrawScope.draw()
}