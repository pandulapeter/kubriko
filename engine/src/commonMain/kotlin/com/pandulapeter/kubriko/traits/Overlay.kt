package com.pandulapeter.kubriko.traits

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko

/**
 * Actors that implement this interface get to drawn directly onto the viewport.
 */
interface Overlay {

    /**
     * This number will be used to determine the order of executing the [drawToViewport] function relative to other [Overlay] Actors.
     * Actors with smaller [overlayDrawingOrder]-s get drawn later (on top).
     */
    val overlayDrawingOrder: Float get() = 0f

    /**
     * Invoked by [Kubriko] to draw using the viewport drawing [scope].
     */
    fun drawToViewport(scope: DrawScope)
}