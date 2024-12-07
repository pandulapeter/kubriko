package com.pandulapeter.kubriko.pointerInput

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.actor.Actor

// TODO: Documentation
interface PointerInputAware : Actor {

    fun onPointerReleased(screenOffset: Offset)
}