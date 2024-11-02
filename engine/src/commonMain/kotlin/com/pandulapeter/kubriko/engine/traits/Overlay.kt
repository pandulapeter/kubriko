package com.pandulapeter.kubriko.engine.traits

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Actors that implement this interface get to drawn directly onto the viewport.
 */
interface Overlay {

    /**
     * Implement this function to draw using the viewport drawing [scope].
     */
    fun drawToViewport(scope: DrawScope)
}