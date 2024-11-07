package com.pandulapeter.kubriko.manager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.implementation.manager.ViewportManagerImpl
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
abstract class ViewportManager : Manager() {

    abstract val cameraPosition: StateFlow<SceneOffset> // Center of the viewport
    abstract val size: StateFlow<Size>
    abstract val scaleFactor: StateFlow<Float>
    // TODO: Support rotation
    abstract val topLeft: StateFlow<SceneOffset>
    abstract val bottomRight: StateFlow<SceneOffset>

    abstract fun addToCameraPosition(offset: Offset)

    abstract fun setCameraPosition(position: SceneOffset)

    abstract fun multiplyScaleFactor(scaleFactor: Float)

    companion object {
        fun newInstance(
            viewportEdgeBuffer: ScenePixel = 100f.scenePixel,
        ): ViewportManager = ViewportManagerImpl(
            viewportEdgeBuffer = viewportEdgeBuffer,
        )
    }
}