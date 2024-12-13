package com.pandulapeter.kubriko.pointerInput

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.actor.Actor

// TODO: Documentation
interface PointerInputAware : Actor {

    fun onPointerPress(screenOffset: Offset) = Unit

    fun onPointerMove(screenOffset: Offset) = Unit

    fun onPointerReleased(screenOffset: Offset) = Unit
}