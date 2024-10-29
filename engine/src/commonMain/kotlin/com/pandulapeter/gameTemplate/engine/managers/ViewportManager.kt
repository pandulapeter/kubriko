package com.pandulapeter.gameTemplate.engine.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import kotlinx.coroutines.flow.StateFlow

interface ViewportManager {
    val size: StateFlow<Size>
    val center: StateFlow<WorldCoordinates>
    val scaleFactor: StateFlow<Float>

    fun addToCenter(offset: Offset)

    fun setCenter(position: WorldCoordinates)

    fun multiplyScaleFactor(scaleFactor: Float)
}