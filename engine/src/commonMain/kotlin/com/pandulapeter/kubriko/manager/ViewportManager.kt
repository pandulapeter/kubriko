package com.pandulapeter.kubriko.manager

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.manager.ViewportManagerImpl
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
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

    sealed class AspectRatioMode {
        data object Dynamic : AspectRatioMode()
        data class Stretched(val preferredWidth: Int, val preferredHeight: Int) : AspectRatioMode()
        data class Fixed(val ratio: Float) : AspectRatioMode()
    }

    companion object {
        fun newInstance(
            aspectRatioMode: AspectRatioMode = AspectRatioMode.Dynamic,
            viewportEdgeBuffer: SceneUnit = 0f.sceneUnit,
        ): ViewportManager = ViewportManagerImpl(
            aspectRatioMode = aspectRatioMode,
            viewportEdgeBuffer = viewportEdgeBuffer,
        )
    }
}