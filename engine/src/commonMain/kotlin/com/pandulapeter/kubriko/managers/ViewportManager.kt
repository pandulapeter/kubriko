package com.pandulapeter.kubriko.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
interface ViewportManager {
    val size: StateFlow<Size>
    val cameraPosition: StateFlow<SceneOffset>
    val scaleFactor: StateFlow<Float>

    fun addToCameraPosition(offset: Offset)

    fun setCameraPosition(position: SceneOffset)

    fun multiplyScaleFactor(scaleFactor: Float)
}