package com.pandulapeter.kubriko.engine.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.engine.types.SceneOffset
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
interface ViewportManager {
    val size: StateFlow<Size>
    val center: StateFlow<SceneOffset>
    val scaleFactor: StateFlow<Float>

    fun addToCenter(offset: Offset)

    fun setCenter(position: SceneOffset)

    fun multiplyScaleFactor(scaleFactor: Float)
}